import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Link2,
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
  CheckCircle2,
  ArrowRight,
  Star,
} from "lucide-react";

/**
 * Landing Page
 * Showcases product features, pricing, and social proof
 * Designed to convert visitors into users
 */
export default function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col">
      {/* Navigation */}
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between">
          <div className="flex items-center gap-2">
            <Link2 className="h-6 w-6 text-primary" />
            <span className="text-xl font-bold">URLShort</span>
          </div>
          <nav className="hidden md:flex items-center gap-6">
            <Link href="#features" className="text-sm font-medium hover:text-primary">
              Features
            </Link>
            <Link href="#pricing" className="text-sm font-medium hover:text-primary">
              Pricing
            </Link>
            <Link href="#" className="text-sm font-medium hover:text-primary">
              Docs
            </Link>
          </nav>
          <div className="flex items-center gap-3">
            <Link href="/login">
              <Button variant="ghost">Sign In</Button>
            </Link>
            <Link href="/signup">
              <Button>Get Started</Button>
            </Link>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="container flex flex-col items-center gap-8 py-24 md:py-32">
        <Badge variant="secondary" className="px-4 py-1">
          Trusted by 10,000+ teams worldwide
        </Badge>
        <h1 className="max-w-4xl text-center text-4xl font-bold tracking-tight sm:text-5xl md:text-6xl lg:text-7xl">
          Shorten URLs.
          <br />
          <span className="text-primary">Track Everything.</span>
        </h1>
        <p className="max-w-2xl text-center text-lg text-muted-foreground sm:text-xl">
          Create branded short links, track clicks in real-time, and gain insights into your audience.
          Built for marketing teams, agencies, and businesses that value data.
        </p>
        <div className="flex flex-col gap-3 sm:flex-row">
          <Link href="/signup">
            <Button size="lg" className="gap-2">
              Start Free Trial
              <ArrowRight className="h-4 w-4" />
            </Button>
          </Link>
          <Link href="#pricing">
            <Button size="lg" variant="outline">
              View Pricing
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
              <span className="text-muted-foreground">Original:</span>
              <code className="flex-1 truncate rounded bg-muted px-2 py-1">
                https://example.com/blog/10-ways-to-improve-your-marketing-strategy-in-2024
              </code>
            </div>
            <div className="flex items-center gap-2">
              <ArrowRight className="h-4 w-4 text-muted-foreground" />
            </div>
            <div className="flex items-center gap-2 text-sm">
              <span className="text-muted-foreground">Shortened:</span>
              <code className="flex-1 rounded bg-primary/10 px-2 py-1 text-primary font-medium">
                short.link/marketing-2024
              </code>
              <Button size="sm" variant="secondary">
                Copy
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="border-t bg-muted/50 py-24">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center mb-16">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Everything you need to manage links at scale
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              From simple shortening to enterprise analytics, we've got you covered
            </p>
          </div>

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {/* Feature Cards */}
            {features.map((feature, index) => (
              <Card key={index} className="border-2">
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

      {/* Pricing Section */}
      <section id="pricing" className="py-24">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center mb-16">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Simple, transparent pricing
            </h2>
            <p className="mt-4 text-lg text-muted-foreground">
              Start free, scale as you grow. No hidden fees.
            </p>
          </div>

          <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-4">
            {pricingTiers.map((tier, index) => (
              <Card
                key={index}
                className={tier.featured ? "border-primary shadow-lg" : ""}
              >
                <CardHeader>
                  {tier.featured && (
                    <Badge className="w-fit mb-2">Most Popular</Badge>
                  )}
                  <CardTitle>{tier.name}</CardTitle>
                  <CardDescription>{tier.description}</CardDescription>
                  <div className="mt-4">
                    <span className="text-4xl font-bold">${tier.price}</span>
                    {tier.price > 0 && (
                      <span className="text-muted-foreground">/month</span>
                    )}
                  </div>
                </CardHeader>
                <CardContent>
                  <ul className="space-y-3 text-sm">
                    {tier.features.map((feature, i) => (
                      <li key={i} className="flex items-center gap-2">
                        <CheckCircle2 className="h-4 w-4 text-primary" />
                        <span>{feature}</span>
                      </li>
                    ))}
                  </ul>
                </CardContent>
                <CardFooter>
                  <Link href="/signup" className="w-full">
                    <Button
                      variant={tier.featured ? "default" : "outline"}
                      className="w-full"
                    >
                      {tier.cta}
                    </Button>
                  </Link>
                </CardFooter>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section className="border-t bg-muted/50 py-24">
        <div className="container">
          <div className="mx-auto max-w-2xl text-center mb-16">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Loved by teams everywhere
            </h2>
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {testimonials.map((testimonial, index) => (
              <Card key={index}>
                <CardHeader>
                  <div className="flex gap-1 mb-2">
                    {[...Array(5)].map((_, i) => (
                      <Star
                        key={i}
                        className="h-4 w-4 fill-yellow-400 text-yellow-400"
                      />
                    ))}
                  </div>
                  <CardDescription className="text-base">
                    "{testimonial.quote}"
                  </CardDescription>
                </CardHeader>
                <CardFooter className="flex-col items-start">
                  <p className="font-semibold">{testimonial.author}</p>
                  <p className="text-sm text-muted-foreground">
                    {testimonial.role}
                  </p>
                </CardFooter>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24">
        <div className="container">
          <div className="mx-auto max-w-3xl rounded-2xl bg-primary px-8 py-16 text-center text-primary-foreground">
            <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
              Ready to take control of your links?
            </h2>
            <p className="mt-4 text-lg opacity-90">
              Join thousands of teams using URLShort to manage their links effectively.
            </p>
            <Link href="/signup">
              <Button
                size="lg"
                variant="secondary"
                className="mt-8 gap-2"
              >
                Start Your Free Trial
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t py-12">
        <div className="container">
          <div className="grid gap-8 md:grid-cols-4">
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Link2 className="h-5 w-5 text-primary" />
                <span className="font-bold">URLShort</span>
              </div>
              <p className="text-sm text-muted-foreground">
                Professional URL shortening and link management platform.
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-3">Product</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link href="#" className="hover:text-foreground">Features</Link></li>
                <li><Link href="#" className="hover:text-foreground">Pricing</Link></li>
                <li><Link href="#" className="hover:text-foreground">API</Link></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-3">Company</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link href="#" className="hover:text-foreground">About</Link></li>
                <li><Link href="#" className="hover:text-foreground">Blog</Link></li>
                <li><Link href="#" className="hover:text-foreground">Careers</Link></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-3">Legal</h3>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li><Link href="#" className="hover:text-foreground">Privacy</Link></li>
                <li><Link href="#" className="hover:text-foreground">Terms</Link></li>
                <li><Link href="#" className="hover:text-foreground">Security</Link></li>
              </ul>
            </div>
          </div>
          <div className="mt-12 border-t pt-8 text-center text-sm text-muted-foreground">
            © 2024 URLShort. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}

// Features data
const features = [
  {
    icon: <Link2 className="h-6 w-6 text-primary" />,
    title: "Custom Short Links",
    description: "Create branded, memorable links with custom slugs that reflect your brand.",
  },
  {
    icon: <BarChart3 className="h-6 w-6 text-primary" />,
    title: "Advanced Analytics",
    description: "Track clicks, locations, devices, and referrers with detailed, real-time analytics.",
  },
  {
    icon: <Globe className="h-6 w-6 text-primary" />,
    title: "Custom Domains",
    description: "Use your own domain for short links to maintain brand consistency.",
  },
  {
    icon: <QrCode className="h-6 w-6 text-primary" />,
    title: "QR Code Generation",
    description: "Automatically generate QR codes for every short link you create.",
  },
  {
    icon: <Calendar className="h-6 w-6 text-primary" />,
    title: "Link Expiration",
    description: "Set expiration dates for time-sensitive campaigns and promotions.",
  },
  {
    icon: <Shield className="h-6 w-6 text-primary" />,
    title: "Password Protection",
    description: "Secure your links with password protection for private content.",
  },
  {
    icon: <Users className="h-6 w-6 text-primary" />,
    title: "Team Collaboration",
    description: "Work together with team members in shared workspaces.",
  },
  {
    icon: <Code className="h-6 w-6 text-primary" />,
    title: "Developer API",
    description: "Integrate with your tools using our comprehensive RESTful API.",
  },
  {
    icon: <Zap className="h-6 w-6 text-primary" />,
    title: "Bulk Operations",
    description: "Create, update, or delete multiple links at once with CSV imports.",
  },
  {
    icon: <Smartphone className="h-6 w-6 text-primary" />,
    title: "Mobile Optimized",
    description: "Manage your links on the go with our responsive dashboard.",
  },
  {
    icon: <Lock className="h-6 w-6 text-primary" />,
    title: "Enterprise Security",
    description: "SOC 2 compliant with advanced security features for enterprises.",
  },
  {
    icon: <BarChart3 className="h-6 w-6 text-primary" />,
    title: "Export Data",
    description: "Export analytics data in CSV or JSON format for further analysis.",
  },
];

// Pricing tiers
const pricingTiers = [
  {
    name: "Free",
    description: "Perfect for personal projects",
    price: 0,
    cta: "Get Started",
    featured: false,
    features: [
      "100 links per month",
      "1,000 clicks tracked",
      "Basic analytics",
      "7-day data retention",
      "Community support",
    ],
  },
  {
    name: "Pro",
    description: "For professionals",
    price: 19,
    cta: "Start Free Trial",
    featured: true,
    features: [
      "Unlimited links",
      "50,000 clicks/month",
      "Advanced analytics",
      "Custom domains (1)",
      "1-year data retention",
      "Priority support",
      "API access",
    ],
  },
  {
    name: "Team",
    description: "For growing teams",
    price: 49,
    cta: "Start Free Trial",
    featured: false,
    features: [
      "Everything in Pro",
      "250,000 clicks/month",
      "Custom domains (5)",
      "Team workspaces",
      "Role-based access",
      "SSO integration",
      "Dedicated support",
    ],
  },
  {
    name: "Enterprise",
    description: "For large organizations",
    price: 199,
    cta: "Contact Sales",
    featured: false,
    features: [
      "Everything in Team",
      "Unlimited clicks",
      "Unlimited domains",
      "Advanced security",
      "SLA guarantee",
      "Custom integrations",
      "Account manager",
    ],
  },
];

// Testimonials
const testimonials = [
  {
    quote:
      "URLShort transformed how we track our marketing campaigns. The analytics are incredibly detailed and actionable.",
    author: "Sarah Chen",
    role: "Marketing Director at TechCorp",
  },
  {
    quote:
      "The API integration was seamless. We automated our entire link generation process and saved hours each week.",
    author: "Michael Rodriguez",
    role: "CTO at StartupXYZ",
  },
  {
    quote:
      "Best URL shortener we've used. The custom domains and branded links have really elevated our professional image.",
    author: "Emily Watson",
    role: "Social Media Manager",
  },
];
