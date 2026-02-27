"use client";

import * as React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { Loader2 } from "lucide-react";
import { Logo } from "@/components/logo";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/components/ui/use-toast";
import { useAuthStore } from "@/stores/auth-store";

// Form validation schema
const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  password: z.string().min(6, "Password must be at least 6 characters"),
});

type LoginFormData = z.infer<typeof loginSchema>;

/**
 * Login Page
 * Handles user authentication with email and password
 * Includes form validation, error handling, and loading states
 */
export default function LoginPage() {
  const router = useRouter();
  const { toast } = useToast();
  const login = useAuthStore((state) => state.login);
  const [isLoading, setIsLoading] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);

    try {
      await login(data.email, data.password);

      toast({
        title: "Welcome back!",
        description: "You've successfully signed in.",
      });

      // Redirect to dashboard
      router.push("/app");
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Login failed",
        description: error.message || "Invalid email or password. Please try again.",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* Left branding panel - hidden on mobile */}
      <div className="hidden lg:flex lg:w-1/2 bg-[#191919] text-white p-12 flex-col justify-between">
        <div>
          <Link href="/" className="mb-16 inline-block">
            <Logo size="lg" />
          </Link>
          <h1 className="text-3xl font-semibold tracking-tight mb-3 text-white/95">
            Shorten, track, and optimize your links
          </h1>
          <p className="text-base text-white/50 mb-10 leading-relaxed">
            The modern link management platform for teams and individuals.
          </p>
          <ul className="space-y-3">
            <li className="flex items-center gap-3 text-sm text-white/60">
              <span className="h-1 w-1 rounded-full bg-white/40 shrink-0" />
              Create short, memorable links in seconds
            </li>
            <li className="flex items-center gap-3 text-sm text-white/60">
              <span className="h-1 w-1 rounded-full bg-white/40 shrink-0" />
              Track clicks and analyze performance
            </li>
            <li className="flex items-center gap-3 text-sm text-white/60">
              <span className="h-1 w-1 rounded-full bg-white/40 shrink-0" />
              Collaborate with your team in workspaces
            </li>
            <li className="flex items-center gap-3 text-sm text-white/60">
              <span className="h-1 w-1 rounded-full bg-white/40 shrink-0" />
              Custom slugs and branded short links
            </li>
          </ul>
        </div>
        <p className="text-xs text-white/30">
          Trusted by thousands of teams worldwide
        </p>
      </div>

      {/* Right form panel */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-8">
        <div className="w-full max-w-md space-y-6 animate-fade-in">
          {/* Mobile logo */}
          <div className="lg:hidden flex justify-center mb-4">
            <Link href="/">
              <Logo size="lg" />
            </Link>
          </div>

          <div className="space-y-2">
            <h2 className="text-2xl font-semibold tracking-tight">Welcome back</h2>
            <p className="text-muted-foreground">
              Enter your credentials to access your account
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Email Field */}
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="name@example.com"
                autoComplete="email"
                disabled={isLoading}
                {...register("email")}
                aria-invalid={errors.email ? "true" : "false"}
              />
              {errors.email && (
                <p className="text-sm text-destructive" role="alert">
                  {errors.email.message}
                </p>
              )}
            </div>

            {/* Password Field */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Password</Label>
                <Link
                  href="/forgot-password"
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  Forgot password?
                </Link>
              </div>
              <Input
                id="password"
                type="password"
                placeholder="Enter your password"
                autoComplete="current-password"
                disabled={isLoading}
                {...register("password")}
                aria-invalid={errors.password ? "true" : "false"}
              />
              {errors.password && (
                <p className="text-sm text-destructive" role="alert">
                  {errors.password.message}
                </p>
              )}
            </div>

            {/* Submit Button */}
            <Button
              type="submit"
              className="w-full"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Signing in...
                </>
              ) : (
                "Sign In"
              )}
            </Button>
          </form>

          <p className="text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{" "}
            <Link
              href="/signup"
              className="font-medium underline underline-offset-4 hover:text-foreground"
            >
              Sign up
            </Link>
          </p>

          <p className="text-center text-xs text-muted-foreground">
            By signing in, you agree to our{" "}
            <Link href="#" className="underline hover:text-foreground">
              Terms of Service
            </Link>{" "}
            and{" "}
            <Link href="#" className="underline hover:text-foreground">
              Privacy Policy
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
