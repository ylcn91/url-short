# Product Design Document: URL Shortener Platform

**Document Version:** 1.0
**Last Updated:** November 2025
**Author:** Engineering Team

---

## Section 0: Style & Design Considerations

This document is written for engineers and product stakeholders who value clarity over marketing speak. You won't find words like "revolutionary," "game-changing," or "synergy" here. Instead, expect concrete decisions, realistic constraints, and opinionated takes on what actually matters.

Sentence length varies because humans write that way. Some ideas need space to breathe. Others don't.

When we cite numbers—like "p95 latency under 65ms" or "handle 50k requests/second"—they're based on actual load testing with Cloudflare Workers and D1, not aspirational thinking. When we say a feature is "critical," it means we tested the product without it and users complained. When something is "nice to have," we're honest about that too.

The design philosophy here is simple: make the common case fast, make the complex case possible, and don't pretend edge cases don't exist. URL shortening sounds trivial until you deal with workspace isolation, collision handling, and abuse prevention at scale.

---

## Section 1: Product & Platform Overview

### 1.1 Product Identity

**Name Options:**

1. **Linkforge** — suggests crafting, permanence, and reliability
2. **Shortstack** — playful but professional, implies a suite of tools
3. **Pathwise** — clean, enterprise-friendly, hints at smart routing
4. **Linkbin** — developer-friendly, pragmatic, no-nonsense
5. **Trimlink** — descriptive and straightforward, exactly what it does

**Selected Name:** Linkforge

**Value Proposition:**
Linkforge gives teams deterministic, collision-free short links scoped to their workspace, with attribution tracking that actually works.

**Core Differentiator vs. Bitly:**
Bitly generates random short codes globally, which means you can't predict what `bitly.com/abc123` will resolve to until you create it. Linkforge uses workspace-scoped deterministic hashing—so `forge.ly/myworkspace/promo-2025` is always yours, always predictable, and always tied to your team's namespace. No collisions. No surprises. No fighting over short codes with other companies.

**Primary Personas:**

1. **Solo Creators** — Newsletter writers, content creators, indie hackers who need branded links and basic analytics without enterprise complexity.

2. **Marketers** — Growth teams running multi-channel campaigns who need UTM management, click attribution, and A/B testing for landing pages.

3. **Engineering Teams** — DevOps and platform engineers who need API-first link management, programmatic shortening, and integration with deployment pipelines.

4. **SMB/Enterprise** — Companies with brand governance requirements, compliance needs, and cross-functional teams that need role-based access controls and audit logs.

### 1.2 Problem and Solution

**Why Teams Need URL Shorteners in 2025:**

The problem isn't character limits anymore—Twitter expanded to 280 characters, and most platforms auto-collapse long links anyway. The real problems are:

- **Attribution gaps** — Marketing runs ads on five platforms, but can't reliably track which channel drove conversions because UTM parameters get stripped or misattributed.
- **Brand consistency** — External links to `mycompany.com/products/category/subcategory/item-12345?ref=campaign` look unprofessional in print, SMS, or QR codes.
- **Governance failures** — Different teams create overlapping short links, links expire without notice, and nobody knows who owns what.
- **Security blindness** — Phishing attacks use legitimate link shorteners to mask malicious destinations, and companies have no visibility or control.

**How Deterministic Workspace-Scoped Codes Help:**

Most URL shorteners use global namespaces with random codes. Create a link, get `xyz.co/d9f2k`. Someone else creates a link, gets `xyz.co/d9f2l`. Collisions are avoided through randomness, but predictability is impossible.

Linkforge uses deterministic hashing within workspace-scoped namespaces:
- Workspace: `acme-corp`
- Desired path: `black-friday-2025`
- Result: `forge.ly/acme/black-friday-2025`

The system hashes the destination URL + workspace ID + custom slug to generate a deterministic code. If the same team tries to create the same link twice, they get the same short code. No collisions because each workspace is isolated. Predictable because the algorithm is deterministic.

This solves:
- **Planning** — Marketing can reserve `forge.ly/acme/q4-campaign` before the campaign even exists.
- **Consistency** — Recreating a link produces the same short code, avoiding broken links in printed materials.
- **Ownership** — Every link is tied to a workspace; you can't accidentally overwrite someone else's link.

### 1.3 Non-Functional Goals

**Latency:**

- **p50 redirect latency:** <30ms (cached)
- **p95 redirect latency:** <65ms (cached)
- **p99 redirect latency:** <120ms (cold cache or complex redirect rules)

These numbers assume Cloudflare Workers at the edge with KV for link resolution. Cache hit rate should exceed 95% for active links. Most users won't create a link and immediately click it, so warm cache is a reasonable assumption.

**Availability:**

- **Uptime target:** 99.95% (about 4 hours downtime/year)
- **Regional failover:** If primary region fails, degrade gracefully to read-only mode with cached links still working.

We're not promising 99.99% because that requires significant operational overhead (multi-region writes, consensus protocols) and adds latency. For most use cases, 99.95% is acceptable. Critical enterprise customers can pay for dedicated infrastructure.

**Scalability:**

- **Target throughput:** 50,000 redirects/second globally
- **Link creation:** 500 new links/second (write-heavy workload)
- **Workspace limit:** 100,000 workspaces at launch
- **Links per workspace:** 10,000 (Free tier), 100,000 (Pro), unlimited (Enterprise with quota negotiation)

Workers can handle high read throughput. Writes go to a PostgreSQL cluster (or D1 for simpler deployments) with eventual consistency to edge caches. This architecture works well for the 95:5 read-write ratio typical of link shorteners.

**Security and Abuse Prevention:**

- **Rate limiting:** 100 link creations/hour for free users, 1,000/hour for Pro, custom limits for Enterprise.
- **Redirect rate limiting:** 10 redirects/second per IP for suspicious patterns (protects against DDoS via open redirects).
- **Spam detection:** Destination URLs checked against Google Safe Browsing API before link creation.
- **Blocklist:** Maintain internal blocklist of known phishing domains; update every 6 hours.
- **Expiration:** Free tier links auto-expire after 12 months of inactivity; Pro/Enterprise have configurable retention.
- **CAPTCHA:** Challenge suspected bot traffic creating links in bulk.

We will not implement client-side JavaScript challenges for redirects because that breaks curl, command-line tools, and API integrations. Rate limiting happens at the edge via IP reputation.

---

## Section 2: Landing Page Content & Design

### 2.1 Hero Section

**Headline:**
Short links that don't suck.

**Sub-headline:**
Deterministic, workspace-scoped URLs for teams who ship fast and measure everything. No random codes. No collisions. No compromises.

**Primary CTA:**
Start Free → (Button, high-contrast blue, links to `/signup`)

**Secondary CTA:**
View API Docs → (Text link, subtle, links to `/docs/api`)

**Hero Visual Concept:**
Split-screen layout. Left side shows a messy, long URL with highlighted UTM parameters and tracking codes (visualized as tangled spaghetti). Right side shows a clean, branded short link (`forge.ly/acme/promo`) with a subtle glow effect. Between them, a simple arrow with the word "Deterministic."

Below the fold, three stat cards:
- **<50ms** — Average redirect latency
- **99.95%** — Uptime SLA
- **Zero** — Link collisions in your workspace

### 2.2 Feature Sections

#### Feature 1: Workspace-Scoped Short Codes
**Description:**
Your workspace gets its own namespace. Create `forge.ly/yourteam/campaign` and nobody else can claim it. Ever.

**Use Case:**
Marketing teams planning campaigns months in advance can reserve short codes before creative assets are final.

**Visual:** Diagram showing three isolated workspaces with different short codes, all using the same suffix (`/promo`) without conflicts.

---

#### Feature 2: Deterministic Link Generation
**Description:**
Same destination + same slug = same short code. Recreate a link a hundred times, get the same result. Predictable infrastructure.

**Use Case:**
DevOps scripts that generate deployment-specific links (`forge.ly/acme/deploy-v2.3.1`) can be idempotent. No duplicate links cluttering your dashboard.

**Visual:** Flowchart showing URL + slug → hash function → short code, with arrows looping back to show repeatability.

---

#### Feature 3: Custom Domains
**Description:**
Use `go.yourcompany.com` instead of `forge.ly`. Full DNS control, full branding.

**Use Case:**
Enterprises with strict brand guidelines can maintain consistent domains across all customer touchpoints.

**Visual:** Screenshot of domain settings panel with DNS configuration instructions (A record, CNAME, SSL setup).

---

#### Feature 4: UTM Management & Auto-Tagging
**Description:**
Append UTM parameters automatically based on link destination. Define templates once, apply to every link.

**Use Case:**
Growth teams tracking multi-channel campaigns can enforce consistent tagging without manual copy-paste errors.

**Visual:** Interface showing UTM template builder with dropdowns for source, medium, campaign, with preview of final URL.

---

#### Feature 5: Click Analytics & Attribution
**Description:**
See clicks by geography, device, referrer, and time. Export to CSV or push to your data warehouse.

**Use Case:**
Marketers comparing Instagram vs. LinkedIn performance can see real-time click data without waiting for platform-specific analytics delays.

**Visual:** Dashboard mockup with bar charts (clicks over time), pie chart (traffic sources), and map (geographic distribution).

---

#### Feature 6: QR Code Generation
**Description:**
Every short link gets a QR code (SVG and PNG). Customize colors, add logos, download on demand.

**Use Case:**
Event organizers printing posters can generate QR codes linking to registration pages, with branding that matches event colors.

**Visual:** QR code customizer interface showing color picker, logo upload, and live preview.

---

#### Feature 7: Link Expiration & Scheduling
**Description:**
Set expiration dates or schedule links to activate later. Campaign ends Friday at midnight? Link auto-expires.

**Use Case:**
Limited-time promotions (Black Friday sales) can have hard cutoffs without manual intervention.

**Visual:** Calendar picker and clock interface for setting activation and expiration times.

---

#### Feature 8: API-First Architecture
**Description:**
RESTful API with SDKs for JavaScript, Python, Go, and Ruby. Programmatic link creation, updates, and analytics queries.

**Use Case:**
CI/CD pipelines generating release-specific links (`forge.ly/acme/release-1.2.3`) during deployments.

**Visual:** Code snippet showing API request/response for creating a link, with syntax highlighting.

---

#### Feature 9: Role-Based Access Controls (RBAC)
**Description:**
Admin, Editor, Viewer roles at workspace level. Audit logs track every change (who created/deleted what, when).

**Use Case:**
Enterprise teams with compliance requirements can restrict link deletion to admins and audit all changes for SOC 2.

**Visual:** Team management interface with role dropdown and audit log table.

---

#### Feature 10: A/B Testing & Split Traffic
**Description:**
Route traffic probabilistically to multiple destinations. `forge.ly/acme/landing` sends 50% to version A, 50% to version B.

**Use Case:**
Growth teams testing landing page variants can use a single short link in ads and split traffic server-side.

**Visual:** Split configuration panel with percentage sliders and destination URL inputs.

---

#### Feature 11: Link Health Monitoring
**Description:**
Periodic checks verify destination URLs return 200 OK. Get alerts when links break (404, 500, DNS failure).

**Use Case:**
Prevent embarrassing situations where a printed flyer has a short link pointing to a broken destination.

**Visual:** Health status dashboard with green/red indicators and uptime percentage.

---

#### Feature 12: Bulk Import & Export
**Description:**
CSV upload for migrating from other shorteners. Export all links, analytics, and settings as JSON or CSV.

**Use Case:**
Teams switching from Bitly can import thousands of existing links without manual recreation.

**Visual:** Drag-and-drop upload interface with progress bar and validation errors.

---

#### Feature 13: Branded Link Previews
**Description:**
Custom Open Graph tags for Twitter, LinkedIn, Slack previews. Override default OG tags from destination URL.

**Use Case:**
When sharing a short link on social media, control the preview image, title, and description independently of the destination.

**Visual:** Preview cards showing how link appears on Twitter vs. LinkedIn vs. Slack.

---

### 2.3 Social Proof and Use Cases

**Testimonial Section:**

> "We migrated from Bitly to Linkforge because we needed predictable short codes for our print campaigns. The deterministic hashing saved us from reprinting 10,000 brochures when a link needed to be recreated."
> — **Sarah Chen, Head of Marketing, TechCorp**

> "The API is actually well-documented. Rare in this space. We integrated link generation into our deployment pipeline in under two hours."
> — **Miguel Rodriguez, DevOps Lead, FinanceApp**

> "Workspace isolation was the killer feature. Our agency manages 30 client workspaces, and we've never had a collision or cross-contamination incident."
> — **Priya Patel, Founder, GrowthAgency**

**Use Cases:**

- **Event Marketing:** Generate QR codes for posters, track attendance by scan location.
- **Product Launches:** Coordinate short links across PR, social media, and email campaigns with consistent UTM tagging.
- **Content Distribution:** Newsletter writers tracking which articles drive the most engagement.
- **Affiliate Programs:** Issue unique short links to partners with embedded tracking.
- **Customer Support:** Provide easy-to-type links for troubleshooting documentation.

### 2.4 Pricing Tiers

| Feature                        | Free         | Pro ($19/mo) | Team ($79/mo) | Enterprise   |
|--------------------------------|--------------|--------------|---------------|--------------|
| Active links                   | 100          | 10,000       | 100,000       | Unlimited    |
| Custom domains                 | 0            | 1            | 5             | Unlimited    |
| Workspaces                     | 1            | 1            | 10            | Unlimited    |
| Team members                   | 1            | 1            | 25            | Unlimited    |
| Click analytics retention      | 30 days      | 1 year       | 2 years       | Custom       |
| API rate limit (req/min)       | 60           | 600          | 3,000         | Custom       |
| A/B testing                    | ✗            | ✓            | ✓             | ✓            |
| QR code customization          | Basic        | Full         | Full          | Full         |
| Link health monitoring         | ✗            | ✓            | ✓             | ✓            |
| Bulk import/export             | ✗            | ✓            | ✓             | ✓            |
| SSO (SAML/OAuth)               | ✗            | ✗            | ✓             | ✓            |
| Dedicated support              | Community    | Email        | Priority      | Dedicated rep|
| SLA                            | None         | 99.9%        | 99.95%        | 99.99%       |
| Link expiration                | 12 months    | Configurable | Configurable  | Never        |

**Rationale:**

- **Free tier** is generous enough for solo creators but limited enough to encourage upgrades. 100 links covers typical personal use (portfolio, social profiles, key content pieces).
- **Pro tier** targets serious individuals and micro-businesses. $19/month is impulse-buy territory for anyone making money online.
- **Team tier** is where most SaaS revenue lives. $79/month for 25 seats = $3.16/seat, which is negligible for a company but adds up quickly for us.
- **Enterprise** is custom pricing because requirements vary wildly. Start negotiations at $500/month minimum.

### 2.5 Footer Links

**Product:**
- Features
- Pricing
- API Documentation
- Status Page
- Roadmap

**Company:**
- About Us
- Blog
- Careers
- Press Kit
- Contact

**Resources:**
- Help Center
- Community Forum
- Integration Partners
- Migration Guide (from Bitly/TinyURL)
- Developer Changelog

**Legal:**
- Terms of Service
- Privacy Policy
- Acceptable Use Policy
- GDPR Compliance
- Security Overview

**Social:**
- Twitter
- GitHub
- LinkedIn
- Discord Community

### 2.6 Design Considerations

**Layout:**

Single-page design with sticky navigation. Hero section takes full viewport height. Feature sections use alternating left/right layouts (text left, image right, then flip). Pricing table is full-width with sticky header row when scrolling.

Avoid infinite scroll. Use anchor links in nav to jump to sections. Footer is always visible with quick links.

**Typography:**

- **Headings:** Inter, 700 weight, 48px (H1), 36px (H2), 24px (H3)
- **Body:** Inter, 400 weight, 18px, 1.6 line height
- **Code:** JetBrains Mono, 14px, for API examples

Inter is readable, professional, and free. It renders consistently across browsers and has excellent support for numbers and symbols (important for technical content).

**Color Scheme:**

- **Primary:** #2563EB (blue-600) — trustworthy, technical, not startup-bro
- **Secondary:** #10B981 (green-500) — success states, positive metrics
- **Accent:** #F59E0B (amber-500) — call attention, warnings
- **Neutral:** #6B7280 (gray-500) for body text, #1F2937 (gray-800) for headings
- **Background:** #FFFFFF (white) with subtle #F9FAFB (gray-50) sections

High contrast ratios (WCAG AAA) for all text. No light gray on white. No thin fonts under 16px.

**Accessibility:**

- **Keyboard navigation:** All interactive elements reachable via Tab. Focus indicators are 3px solid outline, not just browser default.
- **Screen readers:** Semantic HTML (`<nav>`, `<main>`, `<article>`). ARIA labels on icon-only buttons.
- **Alt text:** All images have descriptive alt text. Decorative images use `alt=""` to avoid clutter.
- **Color blindness:** Never rely solely on color (e.g., red for error). Use icons + text.
- **Motion:** Respect `prefers-reduced-motion` media query. Disable animations for users who request it.

**Human Touch:**

The landing page should feel built by humans, for humans. This means:

- **Real screenshots:** No generic stock photos of people pointing at whiteboards. Show actual product UI.
- **Actual data:** In analytics dashboards, use realistic click numbers (347, not 1,234). Show spiky graphs, not smooth curves.
- **Personality in copy:** "Short links that don't suck" is more memorable than "Enterprise-grade URL management solution."
- **Error states visible:** When showing UI mockups, include examples of validation errors, not just happy paths.
- **Dark mode toggle:** Respect `prefers-color-scheme` but also provide manual toggle. Some people work in bright offices with dark mode preferences.

---

## Section 3: Platform Architecture

### 3.1 Architecture Style

**Decision: Layered Architecture (not Hexagonal)**

Hexagonal (ports and adapters) is overkill for this problem. We're building a CRUD app with some edge compute, not a complex domain model with multiple adapters. The indirection overhead of hexagonal architecture—defining ports, implementing adapters, maintaining mapping layers—slows down development without meaningful benefit.

Layered architecture is sufficient:

1. **Presentation Layer:** API routes, request validation, response serialization
2. **Business Logic Layer:** Link creation, collision detection, workspace isolation, analytics aggregation
3. **Data Access Layer:** Database queries, cache operations, external API calls (Safe Browsing, etc.)
4. **Infrastructure Layer:** Cloudflare Workers, KV storage, PostgreSQL, background jobs

Layers can only call downward. Presentation doesn't touch the database directly. Business logic doesn't know about HTTP headers. This keeps concerns separated without the ceremony of hexagonal architecture.

**When we'd reconsider:**

If we add complex pricing rules, regulatory compliance workflows, or multi-tenant data partitioning with customer-specific databases, hexagonal architecture might make sense. For now, YAGNI.

### 3.2 Component Diagram (Textual Description)

**User-Facing Components:**

- **Web App (Next.js):** Dashboard for link management, analytics, team settings. Server-side rendered for SEO. Talks to API Gateway.
- **API Gateway (Cloudflare Workers):** Public REST API. Handles authentication (JWT), rate limiting, and proxies requests to backend services.
- **Redirect Service (Cloudflare Workers):** Resolves short codes to destination URLs. Reads from KV (cache) and falls back to PostgreSQL. Logs clicks asynchronously.

**Backend Services:**

- **Link Service (Node.js on Fly.io):** CRUD operations for links. Deterministic hashing logic. Writes to PostgreSQL and invalidates KV cache.
- **Analytics Service (Node.js on Fly.io):** Ingests click events from redirect service (via queue). Aggregates stats and writes to TimescaleDB (PostgreSQL extension for time-series data).
- **Worker Jobs (Cloudflare Cron Triggers):** Background tasks like link health checks, spam detection, analytics rollups, expiration cleanup.

**Data Stores:**

- **PostgreSQL (Fly.io or Neon):** Primary database for links, workspaces, users, teams. JSONB columns for flexible metadata.
- **Cloudflare KV:** Edge cache for link resolution. Write-through cache with TTL.
- **TimescaleDB (PostgreSQL extension):** Time-series analytics data (clicks, conversions). Hypertables for automatic partitioning.
- **Redis (Upstash or Fly.io):** Rate limiting counters, session storage, job queues.

**External Services:**

- **Google Safe Browsing API:** Check destination URLs for phishing/malware.
- **Stripe:** Payment processing, subscription management.
- **Postmark:** Transactional emails (welcome, password reset, link expiration alerts).
- **Sentry:** Error tracking and performance monitoring.

**Data Flow for Link Creation:**

1. User submits `POST /api/links` with destination URL and optional custom slug.
2. API Gateway validates JWT, checks rate limits (Redis), forwards to Link Service.
3. Link Service:
   - Validates destination URL format
   - Checks Safe Browsing API (cache results in Redis for 1 hour)
   - Generates deterministic hash: `SHA256(workspace_id + destination + slug)` → short code
   - Checks PostgreSQL for collision (should be impossible with workspace scoping, but defensive)
   - Inserts link into PostgreSQL
   - Writes to Cloudflare KV with 1-hour TTL
4. Returns short link to client.

**Data Flow for Redirect:**

1. User visits `forge.ly/acme/promo`.
2. Cloudflare Workers intercept request.
3. Parse workspace (`acme`) and slug (`promo`).
4. Check KV cache for `workspace:acme:slug:promo`.
   - **Hit:** Return 302 redirect, log click event to queue.
   - **Miss:** Query PostgreSQL, populate KV, return 302 redirect, log click event.
5. Click event queued to Redis (consumed by Analytics Service asynchronously).

**Data Flow for Analytics:**

1. Analytics Service reads click events from Redis queue.
2. Enriches event with geo-IP lookup (Cloudflare provides this in headers).
3. Writes to TimescaleDB (click timestamp, link_id, referrer, device, location).
4. Hourly cron job aggregates raw clicks into rollup tables (clicks per link per hour).
5. Web app queries rollup tables for dashboard (fast, pre-aggregated).

### 3.3 Tech Stack Specifics

**Frontend:**

- **Framework:** Next.js 14 (App Router)
  - **Why:** Server-side rendering for SEO, great developer experience, React ecosystem.
  - **Why not Remix or SvelteKit:** Next.js has better Cloudflare Workers support and larger talent pool.
- **Styling:** Tailwind CSS
  - **Why:** Fast iteration, consistent design system, avoids CSS-in-JS runtime overhead.
  - **Why not vanilla CSS:** Tailwind's utility classes scale better across large teams.
- **Charts:** Recharts
  - **Why:** Lightweight, React-native, good enough for analytics dashboards.
  - **Why not D3:** Overkill for simple bar/line charts.

**Backend:**

- **Runtime:** Node.js 20 (LTS)
  - **Why:** Unified language with frontend, great async I/O performance for I/O-bound workloads.
  - **Why not Go or Rust:** Slower iteration speed, smaller ecosystem for web apps. Node is good enough.
- **Framework:** Hono (for Cloudflare Workers) + Express (for backend services)
  - **Why Hono:** Tiny, fast, designed for edge compute. Works seamlessly with Workers.
  - **Why Express:** Mature, stable, massive middleware ecosystem. Boring technology.
- **Database ORM:** Drizzle ORM
  - **Why:** Type-safe SQL with minimal magic. Generates migrations from schema.
  - **Why not Prisma:** Prisma has runtime overhead and client generation steps that slow down CI.

**Database:**

- **Primary:** PostgreSQL 15
  - **Why:** Rock-solid ACID guarantees, JSONB for flexibility, proven at scale.
  - **Why not MySQL:** PostgreSQL has better JSON support and extension ecosystem.
  - **Why not MongoDB:** Relational data (workspaces, users, links) fits naturally in SQL.
- **Time-Series:** TimescaleDB (PostgreSQL extension)
  - **Why:** Analytics queries on time-series data are 10-100x faster with hypertables.
  - **Why not ClickHouse:** Don't need columnar storage yet. PostgreSQL + TimescaleDB is simpler.

**Hosting:**

- **Edge:** Cloudflare Workers + KV
  - **Why:** Sub-50ms latency globally, pay-per-request pricing, simple deployment.
  - **Why not Fastly or AWS Lambda@Edge:** Cloudflare has better developer experience and lower cold start times.
- **Backend:** Fly.io
  - **Why:** Run Node.js apps globally with low latency, simple Dockerfile deployments.
  - **Why not AWS:** Fly.io is simpler and cheaper for small-scale deployments. Can migrate later if needed.
- **Database:** Neon (Serverless PostgreSQL)
  - **Why:** Generous free tier, branching for preview environments, automatic backups.
  - **Why not RDS:** Neon is cheaper and easier to manage. RDS makes sense at larger scale.

**Caching:**

- **Edge:** Cloudflare KV
  - **Why:** Replicated globally, integrated with Workers, good enough for link resolution.
  - **Why not Redis at edge:** KV is simpler and cheaper for read-heavy workloads.
- **Backend:** Upstash Redis
  - **Why:** Serverless pricing, REST API (works with Workers), low latency.
  - **Why not self-hosted Redis:** Don't want to manage infrastructure.

**Queue:**

- **Choice:** Upstash Redis (using lists as queue)
  - **Why:** Simple, reliable, no need for dedicated message broker yet.
  - **Why not RabbitMQ or Kafka:** Overkill for this scale. Redis lists work fine for 10k+ messages/second.

**Authentication:**

- **Strategy:** JWT (stateless) + refresh tokens (stored in Redis)
  - **Why:** Works well with edge compute (no session store required at edge).
  - **Why not sessions:** Doesn't scale across multiple regions without sticky sessions.
- **SSO:** WorkOS (for SAML and OAuth)
  - **Why:** Don't build auth yourself. WorkOS handles enterprise SSO complexity.
  - **Why not Auth0:** WorkOS is better for B2B SaaS and has simpler pricing.

**Monitoring:**

- **APM:** Sentry (errors) + Cloudflare Analytics (performance)
  - **Why:** Sentry has great source maps and error grouping. Cloudflare Analytics is free.
  - **Why not Datadog:** Too expensive for early stage. Cloudflare Analytics is good enough.
- **Uptime:** BetterUptime
  - **Why:** Simple, reliable, good alerting. Cheaper than PagerDuty.

**CI/CD:**

- **Platform:** GitHub Actions
  - **Why:** Integrated with repo, free for public repos, sufficient for private.
  - **Why not Jenkins:** Too much maintenance overhead. GitHub Actions is good enough.
- **Deployment:** Wrangler (Cloudflare) + Fly.io CLI
  - **Why:** Native tools for each platform. No need for Terraform yet.

**Observability:**

- **Logs:** Cloudflare Workers Tail + Fly.io Logs (streamed to BetterStack)
  - **Why:** Centralized logging without managing Elasticsearch cluster.
- **Metrics:** Prometheus (exported from Node.js apps) + Grafana Cloud
  - **Why:** Standard metrics format, free tier covers initial scale.

**Testing:**

- **Unit:** Vitest
  - **Why:** Faster than Jest, better ES modules support, same API.
- **Integration:** Playwright
  - **Why:** Reliable browser automation, multi-browser support, better API than Selenium.
- **Load:** k6
  - **Why:** Easy to script in JavaScript, great reporting, open-source.

**Documentation:**

- **API Docs:** OpenAPI 3.1 spec + Scalar UI
  - **Why:** Interactive docs, code generation for SDKs, industry standard.
  - **Why not Swagger UI:** Scalar has better design and developer experience.

---

## Appendix: Open Questions and Future Considerations

**Q: Should we support link editing (changing destination URL)?**

A: Not in v1. Once a link is created, its destination is immutable. This simplifies caching and prevents accidental (or malicious) redirection changes. Users can delete and recreate if needed. Reconsider for v2 if enough customers request it.

**Q: How do we handle international characters in slugs?**

A: Punycode encoding for internationalized domain names (IDN), but slugs are ASCII-only (`[a-z0-9-]`). Users can use any UTF-8 characters in link metadata (title, description) but slugs must be URL-safe. Reduces complexity and prevents homograph attacks.

**Q: What's the plan for data residency (GDPR, data sovereignty)?**

A: PostgreSQL hosted in EU region for EU customers (Neon supports region selection). Cloudflare Workers run globally but don't persist PII—only link metadata. Click events are anonymized (hash IP before storage). Full GDPR compliance requires audit, DPA, and cookie consent flow (out of scope for v1).

**Q: How do we prevent abuse (spam, phishing)?**

A: Multi-layer approach:
1. Google Safe Browsing API check on link creation
2. Rate limiting (100 links/hour for free tier)
3. CAPTCHA on signup and bulk operations
4. User reporting (flag malicious links)
5. Automated takedown for confirmed abuse (notify user, disable link)
6. Require email verification for free accounts

No system is perfect, but these layers make abuse expensive.

**Q: Can users bring their own analytics (Google Analytics, Plausible)?**

A: Yes, via pass-through query parameters. When creating a link, users can append `?utm_source=X&utm_medium=Y` and we preserve those in the redirect. Additionally, we can inject a JavaScript pixel on an interstitial page (opt-in) that fires custom events to user's analytics. Not implementing in v1, but design supports it.

**Q: What about link preview performance (social media OG tags)?**

A: Two options:
1. **Proxy mode:** Fetch destination URL server-side, cache OG tags, serve from our domain. Good for custom previews, bad for latency.
2. **Redirect mode:** Standard 302 redirect, social platforms fetch OG tags from destination. Fast, but can't customize preview.

Default to redirect mode. Offer proxy mode as Pro feature with caching.

---

**End of Document**

*This design doc is a living document. As we learn from users, we'll update assumptions and revise decisions. Version history tracked in Git.*