"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/layouts/page-header";
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

export default function DashboardPage() {
  const workspace = useAuthStore((state) => state.workspace);
  const { toast } = useToast();

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
        <div className="space-y-1">
          <Skeleton className="h-8 w-40" />
          <Skeleton className="h-4 w-72" />
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[...Array(4)].map((_, i) => (
            <Card key={i}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <Skeleton className="h-4 w-24" />
                <Skeleton className="h-8 w-8 rounded-lg" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-8 w-20 mb-2" />
                <Skeleton className="h-3 w-32" />
              </CardContent>
            </Card>
          ))}
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-4 w-64" />
          </CardHeader>
          <CardContent className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </CardContent>
        </Card>
      </div>
    );
  }

  const statCards = [
    {
      title: "Total Links",
      value: formatNumber(stats?.totalLinks || 0),
      subtitle: `${formatNumber(stats?.activeLinks || 0)} active`,
      trend: "+12%",
      trendUp: true,
      icon: Link2,
    },
    {
      title: "Total Clicks",
      value: formatNumber(stats?.totalClicks || 0),
      subtitle: "All-time clicks",
      trend: "+8%",
      trendUp: true,
      icon: MousePointerClick,
    },
    {
      title: "This Month",
      value: formatNumber(stats?.clicksThisMonth || 0),
      subtitle: `+${formatNumber(stats?.clicksThisWeek || 0)} this week`,
      trend: "+23%",
      trendUp: true,
      icon: TrendingUp,
    },
    {
      title: "Today",
      value: formatNumber(stats?.clicksToday || 0),
      subtitle: "Clicks today",
      trend: "+5%",
      trendUp: true,
      icon: Activity,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Dashboard"
        description="Track your link performance and engagement metrics"
      />

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statCards.map((card, index) => (
          <Card key={card.title} className={`animate-slide-up stagger-${index + 1}`}>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{card.title}</CardTitle>
              <div className="rounded-lg bg-muted p-2">
                <card.icon className="h-4 w-4 text-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex items-baseline gap-2">
                <div className="text-2xl font-bold">{card.value}</div>
                <span className="text-xs text-muted-foreground flex items-center">
                  <TrendingUp className="h-3 w-3 mr-1" />
                  {card.trend}
                </span>
              </div>
              <p className="text-xs text-muted-foreground">
                {card.subtitle}
              </p>
            </CardContent>
          </Card>
        ))}
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
                  <TableRow key={link.id} className="hover:bg-muted/50 transition-colors">
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
                        <span
                          className={`inline-block h-2 w-2 rounded-full mr-1.5 ${
                            link.isActive ? "bg-green-500" : "bg-red-500"
                          }`}
                        />
                        {link.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => handleCopyLink(link.shortCode)}
                          aria-label="Copy link"
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                        <Link href={`/app/links/${link.id}`}>
                          <Button size="sm" variant="ghost" aria-label="View link details">
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
                    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-muted">
                      <MousePointerClick className="h-4 w-4 text-foreground" />
                    </div>
                    <div>
                      <p className="text-sm font-medium">
                        {event.country || "Unknown"} &bull; {event.deviceType}
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
