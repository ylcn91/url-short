"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Link2,
  MousePointerClick,
  TrendingUp,
  Activity,
  ArrowRight,
  ExternalLink,
  Copy,
} from "lucide-react";
import { analyticsApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import { formatNumber, formatRelativeTime, getShortUrl, copyToClipboard } from "@/lib/utils";
import { useToast } from "@/components/ui/use-toast";

/**
 * Dashboard Overview Page
 * Displays key metrics, top links, and recent activity
 * Real-time data fetching with React Query
 */
export default function DashboardPage() {
  const workspace = useAuthStore((state) => state.workspace);
  const { toast } = useToast();

  // Fetch dashboard stats
  const { data: stats, isLoading } = useQuery({
    queryKey: ["dashboard-stats", workspace?.id],
    queryFn: () => analyticsApi.getDashboardStats(workspace!.id),
    enabled: !!workspace,
  });

  const handleCopyLink = async (code: string) => {
    const success = await copyToClipboard(getShortUrl(code));
    if (success) {
      toast({
        title: "Copied!",
        description: "Link copied to clipboard",
      });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader className="animate-pulse">
                <div className="h-4 w-20 bg-muted rounded" />
                <div className="h-8 w-24 bg-muted rounded mt-2" />
              </CardHeader>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Overview</h1>
        <p className="text-muted-foreground">
          Track your link performance and engagement metrics
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Links</CardTitle>
            <Link2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.totalLinks || 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              {formatNumber(stats?.activeLinks || 0)} active
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Clicks</CardTitle>
            <MousePointerClick className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.totalClicks || 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              All-time clicks
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">This Month</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.clicksThisMonth || 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              +{formatNumber(stats?.clicksThisWeek || 0)} this week
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Today</CardTitle>
            <Activity className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.clicksToday || 0)}
            </div>
            <p className="text-xs text-muted-foreground">
              Clicks today
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Top Links */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Top Performing Links</CardTitle>
              <CardDescription>
                Your most clicked links this month
              </CardDescription>
            </div>
            <Link href="/app/links">
              <Button variant="outline" size="sm" className="gap-2">
                View All
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </CardHeader>
        <CardContent>
          {!stats?.topLinks || stats.topLinks.length === 0 ? (
            <div className="text-center py-8">
              <Link2 className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
              <p className="text-muted-foreground mb-4">No links created yet</p>
              <Link href="/app/links/new">
                <Button>Create Your First Link</Button>
              </Link>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Short Link</TableHead>
                  <TableHead>Original URL</TableHead>
                  <TableHead className="text-right">Clicks</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {stats.topLinks.slice(0, 5).map((link) => (
                  <TableRow key={link.id}>
                    <TableCell className="font-medium">
                      <code className="text-sm">{getShortUrl(link.shortCode)}</code>
                    </TableCell>
                    <TableCell className="max-w-xs truncate">
                      {link.originalUrl}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatNumber(link.clickCount)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Badge variant={link.isActive ? "success" : "secondary"}>
                        {link.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleCopyLink(link.shortCode)}
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                        <Link href={`/app/links/${link.id}`}>
                          <Button size="sm" variant="ghost">
                            <ExternalLink className="h-4 w-4" />
                          </Button>
                        </Link>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
          <CardDescription>Latest clicks on your links</CardDescription>
        </CardHeader>
        <CardContent>
          {!stats?.recentActivity || stats.recentActivity.length === 0 ? (
            <p className="text-center text-muted-foreground py-8">
              No recent activity
            </p>
          ) : (
            <div className="space-y-4">
              {stats.recentActivity.slice(0, 8).map((event, index) => (
                <div
                  key={index}
                  className="flex items-center justify-between border-b pb-3 last:border-0"
                >
                  <div className="flex items-center gap-3">
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10">
                      <MousePointerClick className="h-4 w-4 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-medium">
                        {event.country || "Unknown"} • {event.deviceType}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {event.referer || "Direct"}
                      </p>
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {formatRelativeTime(event.timestamp)}
                  </p>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
