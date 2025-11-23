"use client";

import * as React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { ThemeProvider } from "@/components/theme-provider";
import { Toaster } from "@/components/ui/toaster";
import { useAuthStore } from "@/stores/auth-store";

// Create a client outside of the component to avoid recreating on every render
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000, // 1 minute
      retry: 1,
    },
  },
});

/**
 * Root providers component that wraps the entire application
 * Includes React Query for server state and authentication initialization
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const loadAuthFromStorage = useAuthStore((state) => state.loadAuthFromStorage);

  // Load auth state from localStorage on mount
  React.useEffect(() => {
    loadAuthFromStorage();
  }, [loadAuthFromStorage]);

  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryClientProvider client={queryClient}>
        {children}
        <Toaster />
        <ReactQueryDevtools initialIsOpen={false} />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
