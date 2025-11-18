import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "URLShort - Professional URL Shortener",
  description:
    "Create short, trackable links with advanced analytics and custom domains. Built for teams and businesses.",
  keywords: ["url shortener", "link management", "analytics", "marketing"],
};

/**
 * Root layout component
 * Wraps the entire application with providers and base HTML structure
 */
export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={inter.className}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
