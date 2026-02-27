"use client";

import * as React from "react";
import Link from "next/link";
import { cn } from "@/lib/utils";
import { Logo } from "@/components/logo";
import { ThemeToggle } from "@/components/theme-toggle";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Copy,
  Check,
  ArrowRight,
  ExternalLink,
} from "lucide-react";

// --- Code Block with Copy ---

function CodeBlock({
  children,
  language,
  title,
}: {
  children: string;
  language?: string;
  title?: string;
}) {
  const [copied, setCopied] = React.useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(children.trim());
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="group relative rounded-lg border bg-[#1a1a1a] dark:bg-[#0d0d0d] overflow-hidden my-4">
      {title && (
        <div className="flex items-center justify-between border-b border-white/10 px-4 py-2">
          <span className="text-xs font-medium text-white/50">{title}</span>
          {language && (
            <span className="text-xs text-white/30">{language}</span>
          )}
        </div>
      )}
      <div className="relative">
        <pre className="overflow-x-auto p-4 text-sm leading-relaxed text-white/85">
          <code>{children.trim()}</code>
        </pre>
        <button
          onClick={handleCopy}
          className="absolute right-3 top-3 rounded-md p-1.5 text-white/30 opacity-0 transition-opacity hover:text-white/60 group-hover:opacity-100"
        >
          {copied ? (
            <Check className="h-4 w-4" />
          ) : (
            <Copy className="h-4 w-4" />
          )}
        </button>
      </div>
    </div>
  );
}

// --- Endpoint Block ---

function Endpoint({
  method,
  path,
  description,
  auth,
  children,
}: {
  method: "GET" | "POST" | "PATCH" | "DELETE";
  path: string;
  description: string;
  auth?: boolean;
  children?: React.ReactNode;
}) {
  const methodColors: Record<string, string> = {
    GET: "bg-emerald-500/10 text-emerald-700 dark:text-emerald-400",
    POST: "bg-blue-500/10 text-blue-700 dark:text-blue-400",
    PATCH: "bg-amber-500/10 text-amber-700 dark:text-amber-400",
    DELETE: "bg-red-500/10 text-red-700 dark:text-red-400",
  };

  return (
    <div className="rounded-lg border p-4 my-4">
      <div className="flex flex-wrap items-center gap-2 mb-2">
        <span
          className={cn(
            "inline-flex items-center rounded-md px-2 py-0.5 text-xs font-bold font-mono",
            methodColors[method]
          )}
        >
          {method}
        </span>
        <code className="text-sm font-mono text-foreground">{path}</code>
        {auth && (
          <Badge variant="outline" className="text-xs">
            Auth Required
          </Badge>
        )}
      </div>
      <p className="text-sm text-muted-foreground">{description}</p>
      {children}
    </div>
  );
}

// --- Sidebar Sections ---

const sections = [
  { id: "overview", label: "Overview" },
  { id: "base-url", label: "Base URL" },
  { id: "authentication", label: "Authentication" },
  { id: "auth-login", label: "Login", indent: true },
  { id: "auth-signup", label: "Sign Up", indent: true },
  { id: "auth-refresh", label: "Refresh Token", indent: true },
  { id: "auth-me", label: "Current User", indent: true },
  { id: "links", label: "Links" },
  { id: "links-create", label: "Create Link", indent: true },
  { id: "links-list", label: "List Links", indent: true },
  { id: "links-get", label: "Get Link", indent: true },
  { id: "links-update", label: "Update Link", indent: true },
  { id: "links-delete", label: "Delete Link", indent: true },
  { id: "links-bulk", label: "Bulk Create", indent: true },
  { id: "analytics", label: "Analytics" },
  { id: "analytics-stats", label: "Link Stats", indent: true },
  { id: "redirect", label: "Redirect" },
  { id: "workspaces", label: "Workspaces" },
  { id: "ws-current", label: "Current Workspace", indent: true },
  { id: "ws-update", label: "Update Workspace", indent: true },
  { id: "ws-members", label: "List Members", indent: true },
  { id: "ws-add-member", label: "Add Member", indent: true },
  { id: "errors", label: "Error Handling" },
  { id: "rate-limiting", label: "Rate Limiting" },
];

export default function DocsPage() {
  const [activeSection, setActiveSection] = React.useState("overview");

  React.useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setActiveSection(entry.target.id);
          }
        }
      },
      { rootMargin: "-80px 0px -70% 0px", threshold: 0 }
    );

    for (const section of sections) {
      const el = document.getElementById(section.id);
      if (el) observer.observe(el);
    }

    return () => observer.disconnect();
  }, []);

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 lg:px-8">
          <div className="flex items-center gap-6">
            <Link href="/">
              <Logo size="default" />
            </Link>
            <Separator orientation="vertical" className="h-5" />
            <span className="text-sm font-medium text-muted-foreground">
              API Documentation
            </span>
          </div>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Link href="/login">
              <Button variant="ghost" size="sm">
                Sign In
              </Button>
            </Link>
            <Link href="/signup">
              <Button size="sm">Get Started</Button>
            </Link>
          </div>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl">
        {/* Sidebar Navigation */}
        <aside className="hidden lg:block w-64 shrink-0">
          <nav className="sticky top-14 h-[calc(100vh-3.5rem)] overflow-y-auto py-8 pl-8 pr-4">
            <ul className="space-y-0.5">
              {sections.map((section) => (
                <li key={section.id}>
                  <a
                    href={`#${section.id}`}
                    className={cn(
                      "block rounded-md px-3 py-1.5 text-sm transition-colors",
                      section.indent && "ml-4",
                      activeSection === section.id
                        ? "font-medium text-foreground bg-muted"
                        : "text-muted-foreground hover:text-foreground"
                    )}
                    onClick={() => setActiveSection(section.id)}
                  >
                    {section.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 min-w-0 px-4 py-8 lg:px-12 lg:py-12 lg:border-l">
          <div className="max-w-3xl">
            {/* Overview */}
            <section id="overview" className="scroll-mt-20">
              <h1 className="text-3xl font-bold tracking-tight mb-3">
                API Reference
              </h1>
              <p className="text-lg text-muted-foreground mb-6 leading-relaxed">
                The urlshort API lets you create, manage, and track short links
                programmatically. All API endpoints return JSON and use standard
                HTTP response codes.
              </p>

              <div className="grid gap-3 sm:grid-cols-3 mb-8">
                <div className="rounded-lg border p-4">
                  <p className="text-sm font-medium mb-1">REST API</p>
                  <p className="text-xs text-muted-foreground">
                    JSON request and response bodies
                  </p>
                </div>
                <div className="rounded-lg border p-4">
                  <p className="text-sm font-medium mb-1">JWT Auth</p>
                  <p className="text-xs text-muted-foreground">
                    Bearer token authentication
                  </p>
                </div>
                <div className="rounded-lg border p-4">
                  <p className="text-sm font-medium mb-1">Rate Limited</p>
                  <p className="text-xs text-muted-foreground">
                    Fair usage limits per API key
                  </p>
                </div>
              </div>

              <p className="text-sm text-muted-foreground">
                All responses follow a consistent envelope format:
              </p>
              <CodeBlock language="json" title="Response Format">
{`{
  "success": true,
  "data": { ... },
  "message": "Optional status message"
}`}
              </CodeBlock>
            </section>

            <Separator className="my-10" />

            {/* Base URL */}
            <section id="base-url" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Base URL
              </h2>
              <p className="text-sm text-muted-foreground mb-4">
                All API requests should be made to:
              </p>
              <CodeBlock title="Base URL">
{`https://your-domain.com/api/v1`}
              </CodeBlock>
              <p className="text-sm text-muted-foreground">
                For local development, use{" "}
                <code className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono">
                  http://localhost:8080/api/v1
                </code>
              </p>
            </section>

            <Separator className="my-10" />

            {/* Authentication */}
            <section id="authentication" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Authentication
              </h2>
              <p className="text-sm text-muted-foreground mb-4">
                The API uses JWT (JSON Web Tokens) for authentication. Include
                your access token in the{" "}
                <code className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono">
                  Authorization
                </code>{" "}
                header of every request.
              </p>
              <CodeBlock title="Authorization Header">
{`Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...`}
              </CodeBlock>
              <p className="text-sm text-muted-foreground mb-6">
                Access tokens expire after 15 minutes. Use the refresh token
                endpoint to get a new access token without re-authenticating.
              </p>

              {/* Login */}
              <div id="auth-login" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/auth/login"
                  description="Authenticate with email and password to receive JWT tokens."
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "your-password"
}`}
                  </CodeBlock>
                  <CodeBlock language="json" title="Response · 200 OK">
{`{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g...",
    "tokenType": "Bearer",
    "expiresIn": 900000,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "fullName": "Jane Doe",
      "role": "ADMIN",
      "workspaceId": 1,
      "workspaceName": "My Workspace",
      "workspaceSlug": "my-workspace"
    }
  }
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* Signup */}
              <div id="auth-signup" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/auth/signup"
                  description="Create a new account and workspace."
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/auth/signup
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecureP@ss1",
  "fullName": "Jane Doe",
  "workspaceName": "My Workspace",
  "workspaceSlug": "my-workspace"
}`}
                  </CodeBlock>
                  <p className="text-xs text-muted-foreground mt-3">
                    Password must be at least 8 characters with one uppercase,
                    one lowercase, and one number.
                    Returns the same response format as login.
                  </p>
                </Endpoint>
              </div>

              {/* Refresh */}
              <div id="auth-refresh" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/auth/refresh"
                  description="Get a new access token using your refresh token."
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2g..."
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* Me */}
              <div id="auth-me" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/auth/me"
                  description="Get the currently authenticated user's profile."
                  auth
                >
                  <CodeBlock language="json" title="Response · 200 OK">
{`{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Jane Doe",
    "role": "ADMIN",
    "workspaceId": 1,
    "workspaceName": "My Workspace",
    "workspaceSlug": "my-workspace",
    "createdAt": "2025-01-15T10:30:00Z"
  }
}`}
                  </CodeBlock>
                </Endpoint>
              </div>
            </section>

            <Separator className="my-10" />

            {/* Links */}
            <section id="links" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Links
              </h2>
              <p className="text-sm text-muted-foreground mb-6">
                Create and manage short links. Links are scoped to your
                workspace — the same URL will always return the same short code
                within a workspace (deterministic shortening).
              </p>

              {/* Create */}
              <div id="links-create" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/links"
                  description="Create a new short link."
                  auth
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/links
Authorization: Bearer <token>
Content-Type: application/json

{
  "original_url": "https://example.com/very/long/url",
  "custom_code": "my-link",
  "expires_at": "2026-12-31T23:59:59",
  "max_clicks": 1000,
  "tags": ["marketing", "campaign-1"]
}`}
                  </CodeBlock>
                  <div className="mt-3 space-y-1.5">
                    <p className="text-xs text-muted-foreground">
                      <code className="rounded bg-muted px-1 py-0.5 font-mono">custom_code</code>,{" "}
                      <code className="rounded bg-muted px-1 py-0.5 font-mono">expires_at</code>,{" "}
                      <code className="rounded bg-muted px-1 py-0.5 font-mono">max_clicks</code>, and{" "}
                      <code className="rounded bg-muted px-1 py-0.5 font-mono">tags</code> are optional.
                    </p>
                  </div>
                  <CodeBlock language="json" title="Response · 201 Created">
{`{
  "success": true,
  "data": {
    "id": 42,
    "short_code": "my-link",
    "short_url": "https://your-domain.com/my-link",
    "original_url": "https://example.com/very/long/url",
    "created_at": "2025-11-15T14:30:00",
    "expires_at": "2026-12-31T23:59:59",
    "click_count": 0,
    "is_active": true,
    "tags": ["marketing", "campaign-1"]
  },
  "message": "Short link created successfully"
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* List */}
              <div id="links-list" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/links"
                  description="List all links in the workspace with pagination."
                  auth
                >
                  <CodeBlock language="bash" title="Request">
{`GET /api/v1/links?page=0&size=20&sortBy=createdAt&sortDirection=desc
Authorization: Bearer <token>`}
                  </CodeBlock>
                  <div className="mt-3 rounded-lg border">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b">
                          <th className="px-4 py-2 text-left font-medium text-muted-foreground">Parameter</th>
                          <th className="px-4 py-2 text-left font-medium text-muted-foreground">Default</th>
                          <th className="px-4 py-2 text-left font-medium text-muted-foreground">Description</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">page</td>
                          <td className="px-4 py-2 text-muted-foreground">0</td>
                          <td className="px-4 py-2 text-muted-foreground">Page number (0-indexed)</td>
                        </tr>
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">size</td>
                          <td className="px-4 py-2 text-muted-foreground">20</td>
                          <td className="px-4 py-2 text-muted-foreground">Items per page (max 100)</td>
                        </tr>
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">sortBy</td>
                          <td className="px-4 py-2 text-muted-foreground">createdAt</td>
                          <td className="px-4 py-2 text-muted-foreground">Field to sort by</td>
                        </tr>
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">sortDirection</td>
                          <td className="px-4 py-2 text-muted-foreground">desc</td>
                          <td className="px-4 py-2 text-muted-foreground">asc or desc</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </Endpoint>
              </div>

              {/* Get by code */}
              <div id="links-get" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/links/code/{code}"
                  description="Get a specific link by its short code."
                  auth
                >
                  <CodeBlock language="bash" title="Request">
{`GET /api/v1/links/code/my-link
Authorization: Bearer <token>`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* Update */}
              <div id="links-update" className="scroll-mt-20">
                <Endpoint
                  method="PATCH"
                  path="/api/v1/links/{id}"
                  description="Update an existing link. All fields are optional — only send what you want to change."
                  auth
                >
                  <CodeBlock language="json" title="Request">
{`PATCH /api/v1/links/42
Authorization: Bearer <token>
Content-Type: application/json

{
  "original_url": "https://example.com/new-destination",
  "is_active": false,
  "max_clicks": 5000
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* Delete */}
              <div id="links-delete" className="scroll-mt-20">
                <Endpoint
                  method="DELETE"
                  path="/api/v1/links/{id}"
                  description="Soft delete a link. The link will no longer redirect but analytics data is preserved."
                  auth
                >
                  <CodeBlock language="bash" title="Request">
{`DELETE /api/v1/links/42
Authorization: Bearer <token>`}
                  </CodeBlock>
                </Endpoint>
              </div>

              {/* Bulk Create */}
              <div id="links-bulk" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/links/bulk"
                  description="Create multiple short links at once."
                  auth
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/links/bulk
Authorization: Bearer <token>
Content-Type: application/json

{
  "urls": [
    "https://example.com/page-1",
    "https://example.com/page-2",
    "https://example.com/page-3"
  ]
}`}
                  </CodeBlock>
                </Endpoint>
              </div>
            </section>

            <Separator className="my-10" />

            {/* Analytics */}
            <section id="analytics" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Analytics
              </h2>
              <p className="text-sm text-muted-foreground mb-6">
                Every click is tracked automatically. Analytics include
                geographic location, device type, browser, referrer, and
                time-series data.
              </p>

              <div id="analytics-stats" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/links/{id}/stats"
                  description="Get detailed analytics for a specific link."
                  auth
                >
                  <CodeBlock language="json" title="Response · 200 OK">
{`{
  "success": true,
  "data": {
    "short_code": "my-link",
    "total_clicks": 1247,
    "clicks_by_date": {
      "2025-11-15": 45,
      "2025-11-16": 67,
      "2025-11-17": 52
    },
    "clicks_by_country": {
      "US": 450,
      "GB": 320,
      "DE": 180
    },
    "clicks_by_referrer": {
      "google.com": 230,
      "twitter.com": 180,
      "direct": 420
    },
    "clicks_by_device": {
      "mobile": 890,
      "desktop": 580,
      "tablet": 77
    }
  }
}`}
                  </CodeBlock>
                </Endpoint>
              </div>
            </section>

            <Separator className="my-10" />

            {/* Redirect */}
            <section id="redirect" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Redirect
              </h2>
              <p className="text-sm text-muted-foreground mb-6">
                Short links redirect visitors to the original URL. This is a
                public endpoint — no authentication required.
              </p>
              <Endpoint
                method="GET"
                path="/{code}"
                description="Redirect to the original URL. Returns 302 Found with Location header."
              >
                <CodeBlock language="bash" title="Example">
{`curl -I https://your-domain.com/my-link

HTTP/1.1 302 Found
Location: https://example.com/very/long/url
Cache-Control: no-cache, no-store, must-revalidate`}
                </CodeBlock>
                <p className="text-xs text-muted-foreground mt-3">
                  Each redirect is tracked asynchronously — IP, user agent,
                  referrer, and device info are recorded without slowing down the
                  redirect.
                </p>
              </Endpoint>
            </section>

            <Separator className="my-10" />

            {/* Workspaces */}
            <section id="workspaces" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Workspaces
              </h2>
              <p className="text-sm text-muted-foreground mb-6">
                Workspaces isolate links and team members. Each user belongs to
                one workspace.
              </p>

              <div id="ws-current" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/workspaces/current"
                  description="Get the current user's workspace."
                  auth
                >
                  <CodeBlock language="json" title="Response · 200 OK">
{`{
  "success": true,
  "data": {
    "id": 1,
    "name": "My Workspace",
    "slug": "my-workspace",
    "is_active": true,
    "settings": {
      "custom_domain": null,
      "allow_custom_codes": true,
      "max_links_per_user": 1000
    }
  }
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              <div id="ws-update" className="scroll-mt-20">
                <Endpoint
                  method="PATCH"
                  path="/api/v1/workspaces/{id}"
                  description="Update workspace settings. Admin only."
                  auth
                >
                  <CodeBlock language="json" title="Request">
{`PATCH /api/v1/workspaces/1
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Renamed Workspace",
  "settings": {
    "allow_custom_codes": true,
    "max_links_per_user": 500
  }
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              <div id="ws-members" className="scroll-mt-20">
                <Endpoint
                  method="GET"
                  path="/api/v1/workspaces/{id}/members"
                  description="List all members of a workspace."
                  auth
                >
                  <CodeBlock language="json" title="Response · 200 OK">
{`{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "admin@example.com",
      "full_name": "Jane Doe",
      "role": "ADMIN",
      "is_active": true
    },
    {
      "id": 2,
      "email": "member@example.com",
      "full_name": "John Smith",
      "role": "MEMBER",
      "is_active": true
    }
  ]
}`}
                  </CodeBlock>
                </Endpoint>
              </div>

              <div id="ws-add-member" className="scroll-mt-20">
                <Endpoint
                  method="POST"
                  path="/api/v1/workspaces/{id}/members"
                  description="Invite a new member to the workspace. Admin only."
                  auth
                >
                  <CodeBlock language="json" title="Request">
{`POST /api/v1/workspaces/1/members
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "new-member@example.com",
  "full_name": "Alice Johnson",
  "role": "MEMBER"
}`}
                  </CodeBlock>
                  <div className="mt-3 rounded-lg border">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b">
                          <th className="px-4 py-2 text-left font-medium text-muted-foreground">Role</th>
                          <th className="px-4 py-2 text-left font-medium text-muted-foreground">Permissions</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y">
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">ADMIN</td>
                          <td className="px-4 py-2 text-muted-foreground">Full access — manage workspace, members, and all links</td>
                        </tr>
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">MEMBER</td>
                          <td className="px-4 py-2 text-muted-foreground">Create and manage own links, view analytics</td>
                        </tr>
                        <tr>
                          <td className="px-4 py-2 font-mono text-xs">VIEWER</td>
                          <td className="px-4 py-2 text-muted-foreground">Read-only access to links and analytics</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </Endpoint>
              </div>
            </section>

            <Separator className="my-10" />

            {/* Error Handling */}
            <section id="errors" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Error Handling
              </h2>
              <p className="text-sm text-muted-foreground mb-4">
                The API uses standard HTTP status codes. Errors return a
                consistent JSON structure.
              </p>
              <CodeBlock language="json" title="Error Response">
{`{
  "success": false,
  "data": null,
  "message": "Short link not found"
}`}
              </CodeBlock>
              <div className="mt-4 rounded-lg border">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Code</th>
                      <th className="px-4 py-2 text-left font-medium text-muted-foreground">Meaning</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">200</td>
                      <td className="px-4 py-2 text-muted-foreground">Success</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">201</td>
                      <td className="px-4 py-2 text-muted-foreground">Created — resource was successfully created</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">400</td>
                      <td className="px-4 py-2 text-muted-foreground">Bad Request — validation error or malformed input</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">401</td>
                      <td className="px-4 py-2 text-muted-foreground">Unauthorized — missing or invalid token</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">403</td>
                      <td className="px-4 py-2 text-muted-foreground">Forbidden — insufficient permissions</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">404</td>
                      <td className="px-4 py-2 text-muted-foreground">Not Found — resource doesn&apos;t exist</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">409</td>
                      <td className="px-4 py-2 text-muted-foreground">Conflict — duplicate resource (email, slug, etc.)</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">410</td>
                      <td className="px-4 py-2 text-muted-foreground">Gone — link expired or max clicks exceeded</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">429</td>
                      <td className="px-4 py-2 text-muted-foreground">Too Many Requests — rate limit exceeded</td>
                    </tr>
                    <tr>
                      <td className="px-4 py-2 font-mono text-xs">500</td>
                      <td className="px-4 py-2 text-muted-foreground">Internal Server Error</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <Separator className="my-10" />

            {/* Rate Limiting */}
            <section id="rate-limiting" className="scroll-mt-20">
              <h2 className="text-xl font-semibold tracking-tight mb-3">
                Rate Limiting
              </h2>
              <p className="text-sm text-muted-foreground mb-4">
                API requests are rate limited to ensure fair usage. When you
                exceed the limit, the API returns{" "}
                <code className="rounded bg-muted px-1.5 py-0.5 text-xs font-mono">
                  429 Too Many Requests
                </code>
                .
              </p>
              <p className="text-sm text-muted-foreground">
                Rate limit headers are included in every response:
              </p>
              <CodeBlock language="http" title="Rate Limit Headers">
{`X-Rate-Limit-Remaining: 95
X-Rate-Limit-Retry-After-Seconds: 60`}
              </CodeBlock>
            </section>

            {/* CTA */}
            <div className="mt-16 rounded-lg border bg-muted/50 p-8 text-center">
              <h3 className="text-lg font-semibold mb-2">Ready to get started?</h3>
              <p className="text-sm text-muted-foreground mb-4">
                Create your free account and start shortening links in seconds.
              </p>
              <div className="flex items-center justify-center gap-3">
                <Link href="/signup">
                  <Button>
                    Create Account
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Button>
                </Link>
                <a
                  href="http://localhost:8080/swagger-ui.html"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Button variant="outline">
                    Swagger UI
                    <ExternalLink className="ml-2 h-3.5 w-3.5" />
                  </Button>
                </a>
              </div>
            </div>

            <div className="h-20" />
          </div>
        </main>
      </div>
    </div>
  );
}
