# Frontend Documentation

URLShort frontend built with Next.js 14, TypeScript, Tailwind CSS, and shadcn/ui.

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Next.js 14 | React framework with App Router |
| TypeScript | Type safety |
| Tailwind CSS | Utility-first styling |
| shadcn/ui | Component library |
| Zustand | State management |
| Lucide React | Icon library |

## Getting Started

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Project Structure

```
frontend/src/
├── app/                    # Next.js App Router pages
│   ├── page.tsx           # Landing page
│   ├── login/page.tsx     # Login page
│   ├── signup/page.tsx    # Signup page
│   ├── docs/page.tsx      # API documentation page
│   └── app/               # Authenticated dashboard
│       ├── page.tsx       # Dashboard home
│       ├── layout.tsx     # Dashboard layout with sidebar
│       ├── links/         # Link management
│       │   ├── page.tsx   # Links list
│       │   └── [id]/      # Link detail (dynamic route)
│       ├── analytics/     # Analytics views
│       ├── account/       # Account settings
│       ├── team/          # Team management
│       └── workspace/     # Workspace settings
├── components/
│   ├── layouts/           # Layout components
│   │   ├── sidebar.tsx    # Dashboard sidebar navigation
│   │   ├── dashboard-header.tsx
│   │   ├── dashboard-shell.tsx
│   │   ├── page-header.tsx
│   │   └── protected-route.tsx
│   ├── ui/                # shadcn/ui primitives
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── input.tsx
│   │   ├── badge.tsx
│   │   └── ...
│   ├── link-create-modal.tsx
│   ├── logo.tsx
│   └── theme-toggle.tsx
├── lib/
│   ├── auth.ts            # API client for authentication
│   ├── types.ts           # TypeScript type definitions
│   └── utils.ts           # Utility functions (cn, formatters)
├── stores/
│   └── auth-store.ts      # Zustand auth state management
├── globals.css            # Global styles + Tailwind
└── tailwind.config.ts     # Tailwind configuration
```

## Pages

### Public Pages

| Route | Description |
|-------|-------------|
| `/` | Landing page with features, pricing, testimonials |
| `/login` | Email/password login |
| `/signup` | Account registration with workspace creation |
| `/docs` | Interactive API documentation |

### Authenticated Pages (Dashboard)

| Route | Description |
|-------|-------------|
| `/app` | Dashboard overview with stats |
| `/app/links` | Link list with search, filter, pagination |
| `/app/links/[id]` | Link detail with analytics |
| `/app/analytics` | Global analytics dashboard |
| `/app/account` | User profile settings |
| `/app/team` | Team member management |
| `/app/workspace/settings` | Workspace configuration |

## Authentication

Authentication uses JWT tokens managed via Zustand store (`auth-store.ts`).

**Flow:**
1. User logs in via `/login` → receives `accessToken` + `refreshToken`
2. Tokens stored in Zustand (persisted to localStorage)
3. `ProtectedRoute` component wraps authenticated pages
4. API calls include `Authorization: Bearer <token>` header
5. Token refresh happens automatically on 401 responses

```typescript
// Usage in components
import { useAuthStore } from '@/stores/auth-store';

const { user, token, login, logout } = useAuthStore();
```

## API Client

All backend calls go through `lib/auth.ts`:

```typescript
import { apiClient } from '@/lib/auth';

// Authenticated request
const links = await apiClient.get('/api/v1/links');

// The client automatically:
// - Adds Authorization header
// - Handles token refresh on 401
// - Redirects to /login on auth failure
```

## Theming

Dark/light mode support via `next-themes` + Tailwind CSS variables.

- Toggle component: `components/theme-toggle.tsx`
- CSS variables defined in `globals.css`
- Uses `class` strategy (not `media`)

## GitHub Pages Deployment

The landing page is statically exported and deployed to GitHub Pages.

**Configuration** (`next.config.js`):
- `output: 'export'` only when `GITHUB_PAGES=true`
- `basePath: '/url-short'` for GitHub Pages subpath
- Dynamic routes (`[id]`) are excluded from static export

**Workflow** (`.github/workflows/deploy-github-pages.yml`):
- Triggers on push to `main` (paths: `frontend/**`, `docs/**`)
- Converts `docs/*.md` to HTML via `scripts/build-docs.mjs`
- Builds with `npm run build`
- Deploys `frontend/out/` via `actions/deploy-pages@v4`

## Component Conventions

- Use shadcn/ui components from `components/ui/`
- Compose layouts with shell components from `components/layouts/`
- Client components use `"use client"` directive
- Server components are the default (no directive needed)
- Use `cn()` utility for conditional class names

```typescript
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

<Button className={cn("w-full", isLoading && "opacity-50")} />
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080` | Backend API base URL |
| `GITHUB_PAGES` | — | Set to `true` for static export |

## Building

```bash
# Development
npm run dev

# Production build
npm run build

# Static export (GitHub Pages)
GITHUB_PAGES=true npm run build
# Output: frontend/out/
```
