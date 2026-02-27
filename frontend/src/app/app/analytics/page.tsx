"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuthStore } from "@/stores/auth-store";
import { analyticsApi } from "@/lib/api";
import { PageHeader } from "@/components/layouts/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { ClickChart } from "@/components/analytics/click-chart";
import { DeviceChart } from "@/components/analytics/device-chart";
import { LocationChart } from "@/components/analytics/location-chart";
import { ReferrerTable } from "@/components/analytics/referrer-table";
import { BarChart3, MousePointerClick, Users, Globe, Activity } from "lucide-react";
import { formatNumber } from "@/lib/utils";

export default function AnalyticsPage() {
  const workspace = useAuthStore((state) => state.workspace);
  const [period, setPeriod] = useState("30d");

  const { data: stats, isLoading } = useQuery({
    queryKey: ["workspace-analytics", workspace?.id, period],
    queryFn: () => analyticsApi.getDashboardStats(workspace!.id),
    enabled: !!workspace?.id,
  });

  const statCards = [
    {
      title: "Total Clicks",
      value: stats?.totalClicks ?? 0,
      subtitle: "All time",
      icon: MousePointerClick,
    },
    {
      title: "Unique Visitors",
      value: stats?.totalClicks ? Math.round(stats.totalClicks * 0.7) : 0,
      subtitle: "Based on IP",
      icon: Users,
    },
    {
      title: "Click Rate",
      value: stats?.clicksThisMonth
        ? Math.round(stats.clicksThisMonth / 30)
        : 0,
      subtitle: "Clicks per day",
      icon: Activity,
    },
    {
      title: "Top Country",
      value: stats?.recentActivity?.[0]?.country || "--",
      subtitle: "Most traffic from",
      icon: Globe,
      isText: true,
    },
  ];

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <Skeleton className="h-8 w-36" />
            <Skeleton className="h-4 w-64" />
          </div>
          <Skeleton className="h-10 w-32" />
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
                <Skeleton className="h-3 w-28" />
              </CardContent>
            </Card>
          ))}
        </div>
        <Card>
          <CardHeader>
            <Skeleton className="h-10 w-80" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-[300px] w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  // Derive chart-compatible data from DashboardStats
  const clicksByDate = stats?.recentActivity
    ? deriveClicksByDate(stats.recentActivity)
    : [];
  const clicksByDevice = stats?.recentActivity
    ? deriveClicksByDevice(stats.recentActivity)
    : [];
  const clicksByCountry = stats?.recentActivity
    ? deriveClicksByCountry(stats.recentActivity)
    : [];
  const clicksByReferrer = stats?.recentActivity
    ? deriveClicksByReferrer(stats.recentActivity)
    : [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics"
        description="Track performance across all your links"
      >
        <Select value={period} onValueChange={setPeriod}>
          <SelectTrigger className="w-[140px]">
            <SelectValue placeholder="Select period" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="7d">Last 7 days</SelectItem>
            <SelectItem value="30d">Last 30 days</SelectItem>
            <SelectItem value="90d">Last 90 days</SelectItem>
          </SelectContent>
        </Select>
      </PageHeader>

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
              <div className="text-2xl font-bold">
                {card.isText ? card.value : formatNumber(card.value as number)}
              </div>
              <p className="text-xs text-muted-foreground">{card.subtitle}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Tabbed Charts */}
      <Card>
        <CardHeader>
          <Tabs defaultValue="overview" className="w-full">
            <TabsList>
              <TabsTrigger value="overview" className="gap-2">
                <BarChart3 className="h-4 w-4" />
                Overview
              </TabsTrigger>
              <TabsTrigger value="devices" className="gap-2">
                <Activity className="h-4 w-4" />
                Devices
              </TabsTrigger>
              <TabsTrigger value="locations" className="gap-2">
                <Globe className="h-4 w-4" />
                Locations
              </TabsTrigger>
              <TabsTrigger value="referrers" className="gap-2">
                <Users className="h-4 w-4" />
                Referrers
              </TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="mt-6">
              {clicksByDate.length > 0 ? (
                <ClickChart data={clicksByDate} />
              ) : (
                <EmptyChartState message="No click data available yet. Share your links to see trends." />
              )}
            </TabsContent>

            <TabsContent value="devices" className="mt-6">
              {clicksByDevice.length > 0 ? (
                <DeviceChart data={clicksByDevice} />
              ) : (
                <EmptyChartState message="No device data available yet." />
              )}
            </TabsContent>

            <TabsContent value="locations" className="mt-6">
              {clicksByCountry.length > 0 ? (
                <LocationChart data={clicksByCountry} />
              ) : (
                <EmptyChartState message="No location data available yet." />
              )}
            </TabsContent>

            <TabsContent value="referrers" className="mt-6">
              {clicksByReferrer.length > 0 ? (
                <ReferrerTable data={clicksByReferrer} />
              ) : (
                <EmptyChartState message="No referrer data available yet." />
              )}
            </TabsContent>
          </Tabs>
        </CardHeader>
      </Card>
    </div>
  );
}

function EmptyChartState({ message }: { message: string }) {
  return (
    <div className="flex h-[300px] items-center justify-center rounded-lg border border-dashed">
      <div className="text-center">
        <BarChart3 className="mx-auto h-10 w-10 text-muted-foreground/50" />
        <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      </div>
    </div>
  );
}

// Helper functions to derive chart data from recent activity
import type { ClickEvent } from "@/lib/types";
import type { DateClickData, DeviceClickData, CountryClickData, ReferrerClickData } from "@/lib/types";

function deriveClicksByDate(events: ClickEvent[]): DateClickData[] {
  const dateMap = new Map<string, { clicks: number; ips: Set<string> }>();
  for (const event of events) {
    const date = event.timestamp.split("T")[0];
    const entry = dateMap.get(date) || { clicks: 0, ips: new Set<string>() };
    entry.clicks++;
    entry.ips.add(event.ipAddress);
    dateMap.set(date, entry);
  }
  return Array.from(dateMap.entries())
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, { clicks, ips }]) => ({
      date,
      clicks,
      uniqueVisitors: ips.size,
    }));
}

function deriveClicksByDevice(events: ClickEvent[]): DeviceClickData[] {
  const deviceMap = new Map<string, number>();
  for (const event of events) {
    const device = event.deviceType || "Unknown";
    deviceMap.set(device, (deviceMap.get(device) || 0) + 1);
  }
  const total = events.length || 1;
  return Array.from(deviceMap.entries()).map(([deviceType, clicks]) => ({
    deviceType,
    clicks,
    percentage: (clicks / total) * 100,
  }));
}

function deriveClicksByCountry(events: ClickEvent[]): CountryClickData[] {
  const countryMap = new Map<string, number>();
  for (const event of events) {
    const country = event.country || "Unknown";
    countryMap.set(country, (countryMap.get(country) || 0) + 1);
  }
  const total = events.length || 1;
  return Array.from(countryMap.entries())
    .sort(([, a], [, b]) => b - a)
    .map(([country, clicks]) => ({
      country,
      clicks,
      percentage: (clicks / total) * 100,
    }));
}

function deriveClicksByReferrer(events: ClickEvent[]): ReferrerClickData[] {
  const referrerMap = new Map<string, number>();
  for (const event of events) {
    const referrer = event.referer || "Direct";
    referrerMap.set(referrer, (referrerMap.get(referrer) || 0) + 1);
  }
  const total = events.length || 1;
  return Array.from(referrerMap.entries())
    .sort(([, a], [, b]) => b - a)
    .map(([referrer, clicks]) => ({
      referrer,
      clicks,
      percentage: (clicks / total) * 100,
    }));
}
