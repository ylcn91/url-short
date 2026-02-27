import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ThemeToggle } from "@/components/theme-toggle";
import { Logo } from "@/components/logo";
import {
  BarChart3,
  Globe,
  Shield,
  Zap,
  Users,
  Code,
  QrCode,
  Calendar,
  Lock,
  Smartphone,
  ArrowRight,
  Github,
  BookOpen,
  Terminal,
  Database,
  Activity,
} from "lucide-react";

export default function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col">
      {/* Navigation */}
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between">
          <Logo />
          <nav className="hidden md:flex items-center gap-6">
            <Link href="#features" className="text-sm font-medium hover:text-primary">
              Features
            </Link>
            <Link href="#tech-stack" className="text-sm font-medium hover:text-primary">
              Tech Stack
            </Link>
            <Link href="/docs" className="text-sm font-medium hover:text-primary">
              API Docs
            </Link>
          </nav>
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <a
              href="https://github.com/ylcn91/url-short"
              target="_blank"
              rel="noopener noreferrer"
            >
              <Button variant="outline" className="gap-2">
                <Github className="h-4 w-4" />
                GitHub
              </Button>
            </a>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative">
        <div className="container flex flex-col items-center gap-8 py-24 md:py-32 animate-slide-up">
          <Badge variant="secondary" className="px-4 py-1">
            Open Source URL Shortener
          </Badge>
          <h1 className="max-w-5xl text-center text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
            Connections start
            <br />
            with{" "}
            <span className="bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
              a link
            </span>
          </h1>
          <p className="max-w-3xl text-center text-lg text-muted-foreground sm:text-xl md:text-2xl">
            Self-hosted URL shortener with analytics, custom domains, A/B testing,
            and team workspaces. Built with Spring Boot and Next.js.
          </p>
          <div className="flex flex-col gap-3 sm:flex-row">
            <a
              href="https://github.com/ylcn91/url-short"
              target="_blank"
              rel="noopener noreferrer"
            >
              <Button size="lg" className="gap-2">
                <Github className="h-5 w-5" />
                View on GitHub
              </Button>
            </a>
            <Link href="/docs">
              <Button size="lg" variant="outline" className="gap-2">
                <BookOpen className="h-4 w-4" />
                Read the Docs
              </Button>
            </Link>
          </div>

          {/* Quick Demo */}
          <div className="mt-12 w-full max-w-3xl rounded-lg border bg-card p-6 shadow-lg">
            <div className="flex items-center gap-2 mb-4">
              <div className="flex h-3 w-3 rounded-full bg-red-500" />
              <div className="flex h-3 w-3 rounded-full bg-yellow-500" />
              <div className="flex h-3 w-3 rounded-full bg-green-500" />
            </div>
            <div className="space-y-3">
              <div className="flex items-center gap-2 text-sm">
                <span className="text-muted-foreground font-mono">$</span>
                <code className="flex-1 truncate rounded bg-muted px-2 py-1">
                  docker compose up -d
                </code>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-muted-foreground font-mono">$</span>
                <code className="flex-1 truncate rounded bg-muted px-2 py-1">
                  curl -X POST localhost:8080/api/v1/links -d &apos;{`{"original_url":"https://example.com/very/long/url"}`}&apos;
                </code>
              </div>
              <div className="flex items-center gap-2">
                <ArrowRight className="h-4 w-4 text-muted-foreground" />
              </div>
              <div className="flex items-center gap-2 text-sm">
                <code className="flex-1 rounded bg-primary/10 px-2 py-1 text-primary font-medium">
                  http://localhost:8080/aBc12x
                </code>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Tech Highlights */}
      <section className="border-t py-16">
        <div className="container">
          <div className="grid gap-8 md:grid-cols-4 text-center">
            <div>
              <div className="text-3xl font-bold text-primary">Java 21</div>
              <div className="mt-2 text-sm text-muted-foreground">Spring Boot 3.4</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">Next.js 14</div>
              <div className="mt-2 text-sm text-muted-foreground">TypeScript + Tailwind</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">PostgreSQL</div>
              <div className="mt-2 text-sm text-muted-foreground">Redis + Kafka</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-primary">Docker</div>
              <div className="mt-2 text-sm text-muted-foreground">One command setup</div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="border-t py-24">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center mb-16">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Everything you need to manage links at scale
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              Production-ready features, fully self-hosted
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {features.map((feature, index) => (
              <Card key={index} className="border-2 hover:shadow-lg hover:border-primary/20 transition-shadow duration-200">
                <CardHeader>
                  <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                    {feature.icon}
                  </div>
                  <CardTitle className="mt-4">{feature.title}</CardTitle>
                  <CardDescription>{feature.description}</CardDescription>
                </CardHeader>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Tech Stack Section */}
      <section id="tech-stack" className="border-t bg-muted/50 py-24">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center mb-16">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Built with modern technologies
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              Production-grade stack designed for scalability and developer experience
            </p>
          </div>

          <div className="grid gap-8 md:grid-cols-2">
            <Card className="border-2">
              <CardHeader>
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Terminal className="h-6 w-6 text-primary" />
                </div>
                <CardTitle className="mt-4">Backend</CardTitle>
                <CardDescription className="text-base leading-relaxed">
                  Spring Boot 3.4 on Java 21 with JWT authentication,
                  rate limiting (Bucket4j), Kafka event streaming for click analytics,
                  and Flyway database migrations. Full REST API with OpenAPI docs.
                </CardDescription>
              </CardHeader>
            </Card>

            <Card className="border-2">
              <CardHeader>
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Globe className="h-6 w-6 text-primary" />
                </div>
                <CardTitle className="mt-4">Frontend</CardTitle>
                <CardDescription className="text-base leading-relaxed">
                  Next.js 14 with TypeScript, Tailwind CSS, and shadcn/ui.
                  Zustand for state management, TanStack Query for server state,
                  and Recharts for analytics visualization.
                </CardDescription>
              </CardHeader>
            </Card>

            <Card className="border-2">
              <CardHeader>
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Database className="h-6 w-6 text-primary" />
                </div>
                <CardTitle className="mt-4">Infrastructure</CardTitle>
                <CardDescription className="text-base leading-relaxed">
                  PostgreSQL for primary storage, Redis for caching and rate limiting,
                  Apache Kafka for async click event processing.
                  Full Docker Compose setup with health checks.
                </CardDescription>
              </CardHeader>
            </Card>

            <Card className="border-2">
              <CardHeader>
                <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                  <Activity className="h-6 w-6 text-primary" />
                </div>
                <CardTitle className="mt-4">Testing &amp; CI/CD</CardTitle>
                <CardDescription className="text-base leading-relaxed">
                  JUnit 5, Mockito, Testcontainers, and ArchUnit for backend.
                  GitHub Actions CI/CD with backend, frontend, and integration test pipelines.
                  149+ tests with architecture enforcement.
                </CardDescription>
              </CardHeader>
            </Card>
          </div>
        </div>
      </section>

      {/* Quick Start Section */}
      <section className="border-t py-24">
        <div className="container">
          <div className="mx-auto max-w-3xl">
            <div className="text-center mb-12">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                Get running in 30 seconds
              </h2>
              <p className="mt-4 text-lg text-muted-foreground">
                Clone, compose, done.
              </p>
            </div>

            <div className="rounded-lg border bg-[#1a1a1a] dark:bg-[#0d0d0d] overflow-hidden">
              <div className="flex items-center gap-2 border-b border-white/10 px-4 py-3">
                <div className="flex h-3 w-3 rounded-full bg-red-500/80" />
                <div className="flex h-3 w-3 rounded-full bg-yellow-500/80" />
                <div className="flex h-3 w-3 rounded-full bg-green-500/80" />
                <span className="ml-2 text-xs text-white/40">terminal</span>
              </div>
              <pre className="p-5 text-sm leading-loose text-white/85 overflow-x-auto">
                <code>{`git clone https://github.com/ylcn91/url-short.git
cd url-short
docker compose up -d

# Backend:  http://localhost:8080
# Frontend: http://localhost:3000
# API Docs: http://localhost:8080/swagger-ui.html`}</code>
              </pre>
            </div>

            <div className="mt-8 flex justify-center gap-4">
              <a
                href="https://github.com/ylcn91/url-short"
                target="_blank"
                rel="noopener noreferrer"
              >
                <Button size="lg" className="gap-2">
                  <Github className="h-5 w-5" />
                  Star on GitHub
                </Button>
              </a>
              <Link href="/docs">
                <Button size="lg" variant="outline" className="gap-2">
                  API Reference
                  <ArrowRight className="h-4 w-4" />
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t py-12">
        <div className="container">
          <div className="grid gap-8 md:grid-cols-3">
            <div>
              <div className="mb-4">
                <Logo size="sm" />
              </div>
              <p className="text-sm text-muted-foreground">
                Open source URL shortener and link management platform.
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-3">Documentation</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link href="/docs" className="hover:text-foreground">API Reference</Link></li>
                <li><a href="/url-short/docs/ARCHITECTURE.html" className="hover:text-foreground">Architecture</a></li>
                <li><a href="/url-short/docs/LOCAL_SETUP.html" className="hover:text-foreground">Local Setup</a></li>
                <li><a href="/url-short/docs/DATABASE_SCHEMA.html" className="hover:text-foreground">Database Schema</a></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-3">Project</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><a href="https://github.com/ylcn91/url-short" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">GitHub Repository</a></li>
                <li><a href="https://github.com/ylcn91/url-short/issues" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">Issues</a></li>
                <li><a href="/url-short/CONTRIBUTING.html" className="hover:text-foreground">Contributing</a></li>
                <li><a href="https://github.com/ylcn91/url-short/blob/main/LICENSE" target="_blank" rel="noopener noreferrer" className="hover:text-foreground">MIT License</a></li>
              </ul>
            </div>
          </div>
          <div className="mt-12 border-t pt-8 text-center text-sm text-muted-foreground">
            &copy; {new Date().getFullYear()} URLShort. Open source under MIT License.
          </div>
        </div>
      </footer>
    </div>
  );
}

const features = [
  {
    icon: <Zap className="h-6 w-6 text-primary" />,
    title: "Deterministic Short Codes",
    description: "Same URL always produces the same short code within a workspace. No duplicates, no waste.",
  },
  {
    icon: <BarChart3 className="h-6 w-6 text-primary" />,
    title: "Click Analytics",
    description: "Track clicks, locations, devices, browsers, and referrers with Kafka-powered real-time analytics.",
  },
  {
    icon: <Globe className="h-6 w-6 text-primary" />,
    title: "Custom Domains",
    description: "Use your own domain for branded short links with DNS verification.",
  },
  {
    icon: <QrCode className="h-6 w-6 text-primary" />,
    title: "A/B Testing",
    description: "Split traffic across multiple destinations with weighted routing and conversion tracking.",
  },
  {
    icon: <Calendar className="h-6 w-6 text-primary" />,
    title: "Link Expiration",
    description: "Set expiration dates or max click limits for time-sensitive links.",
  },
  {
    icon: <Shield className="h-6 w-6 text-primary" />,
    title: "Password Protection",
    description: "Secure links with password gates and brute-force protection.",
  },
  {
    icon: <Users className="h-6 w-6 text-primary" />,
    title: "Team Workspaces",
    description: "Multi-tenant workspaces with Admin, Member, and Viewer roles.",
  },
  {
    icon: <Code className="h-6 w-6 text-primary" />,
    title: "REST API",
    description: "Full CRUD API with JWT auth, bulk operations, and OpenAPI documentation.",
  },
  {
    icon: <Lock className="h-6 w-6 text-primary" />,
    title: "Webhooks",
    description: "Real-time notifications on link events with HMAC signature verification.",
  },
  {
    icon: <Smartphone className="h-6 w-6 text-primary" />,
    title: "Link Health Monitoring",
    description: "Automatic dead link detection with health status tracking.",
  },
  {
    icon: <Zap className="h-6 w-6 text-primary" />,
    title: "Rate Limiting",
    description: "Configurable per-endpoint rate limits with Bucket4j and Redis.",
  },
  {
    icon: <BarChart3 className="h-6 w-6 text-primary" />,
    title: "Docker Ready",
    description: "Full Docker Compose stack with PostgreSQL, Redis, Kafka, backend, and frontend.",
  },
];
