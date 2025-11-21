"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Link2,
  BarChart3,
  Settings,
  Users,
  Plus,
  LogOut,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";

/**
 * Sidebar navigation component
 * Displays navigation links for the dashboard
 * Highlights active route
 */
export function Sidebar() {
  const pathname = usePathname();
  const logout = useAuthStore((state) => state.logout);

  const navigation = [
    {
      name: "Dashboard",
      href: "/app",
      icon: LayoutDashboard,
      exact: true,
    },
    {
      name: "Links",
      href: "/app/links",
      icon: Link2,
    },
    {
      name: "Analytics",
      href: "/app/analytics",
      icon: BarChart3,
    },
    {
      name: "Team",
      href: "/app/team",
      icon: Users,
    },
    {
      name: "Settings",
      href: "/app/workspace/settings",
      icon: Settings,
    },
  ];

  const isActive = (href: string, exact?: boolean) => {
    if (exact) {
      return pathname === href;
    }
    return pathname.startsWith(href);
  };

  return (
    <aside className="flex h-screen w-64 flex-col border-r bg-muted/40">
      {/* Logo */}
      <div className="flex h-16 items-center gap-2 border-b px-6">
        <Link2 className="h-6 w-6 text-primary" />
        <span className="text-lg font-bold">URLShort</span>
      </div>

      {/* Quick Action */}
      <div className="p-4">
        <Link href="/app/links/new">
          <Button className="w-full gap-2">
            <Plus className="h-4 w-4" />
            Create Link
          </Button>
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3">
        {navigation.map((item) => {
          const active = isActive(item.href, item.exact);
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                active
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
              )}
            >
              <item.icon className="h-4 w-4" />
              {item.name}
            </Link>
          );
        })}
      </nav>

      {/* User Section */}
      <div className="border-t p-4">
        <Link href="/app/account">
          <div className="mb-2 rounded-lg px-3 py-2 hover:bg-accent cursor-pointer">
            <p className="text-sm font-medium">Account</p>
            <p className="text-xs text-muted-foreground">Manage your profile</p>
          </div>
        </Link>
        <Button
          variant="ghost"
          className="w-full justify-start gap-2"
          onClick={logout}
        >
          <LogOut className="h-4 w-4" />
          Sign Out
        </Button>
      </div>
    </aside>
  );
}
