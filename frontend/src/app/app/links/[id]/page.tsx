"use client";

import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { QRCodeSVG } from "qrcode.react";
import {
  ArrowLeft,
  Copy,
  ExternalLink,
  Edit,
  Trash2,
  Download,
  QrCode,
  BarChart3,
  Globe,
  Smartphone,
  MousePointerClick,
  TrendingUp,
  Calendar,
  Tag,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { useToast } from "@/components/ui/use-toast";
import { linksApi, analyticsApi } from "@/lib/api";
import { formatNumber, formatDate, getShortUrl, copyToClipboard } from "@/lib/utils";
import { ClickChart } from "@/components/analytics/click-chart";
import { DeviceChart } from "@/components/analytics/device-chart";
import { LocationChart } from "@/components/analytics/location-chart";
import { ReferrerTable } from "@/components/analytics/referrer-table";

/**
 * Link Detail Page
 * Displays comprehensive analytics and management options for a single link
 * Includes charts, QR code, and export functionality
 */
export default function LinkDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const linkId = Number(params.id);

  const [dateRange, setDateRange] = React.useState({
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
    endDate: new Date().toISOString().split("T")[0],
  });

  // Fetch link details
  const { data: link, isLoading: linkLoading } = useQuery({
    queryKey: ["link", linkId],
    queryFn: () => linksApi.getLink(linkId),
  });

  // Fetch analytics
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ["link-stats", linkId, dateRange],
    queryFn: () => analyticsApi.getLinkStats(linkId, dateRange),
    enabled: !!link,
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: () => linksApi.deleteLink(linkId),
    onSuccess: () => {
      toast({
        title: "Link deleted",
        description: "The link has been successfully deleted",
      });
      router.push("/app/links");
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Error",
        description: error.message || "Failed to delete link",
      });
    },
  });

  const handleCopyLink = async () => {
    if (!link) return;
    const success = await copyToClipboard(getShortUrl(link.shortCode));
    if (success) {
      toast({
        title: "Copied!",
        description: "Link copied to clipboard",
      });
    }
  };

  const handleDeleteLink = () => {
    if (!confirm("Are you sure you want to delete this link? This action cannot be undone.")) {
      return;
    }
    deleteMutation.mutate();
  };

  const handleExportData = async (format: "csv" | "json") => {
    try {
      const blob = await analyticsApi.exportData(linkId, format);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `link-${link?.shortCode}-analytics.${format}`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      toast({
        title: "Export successful",
        description: `Analytics data exported as ${format.toUpperCase()}`,
      });
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Export failed",
        description: error.message || "Failed to export data",
      });
    }
  };

  if (linkLoading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-48 bg-muted animate-pulse rounded" />
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

  if (!link) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-muted-foreground mb-4">Link not found</p>
        <Link href="/app/links">
          <Button>Back to Links</Button>
        </Link>
      </div>
    );
  }

  const shortUrl = getShortUrl(link.shortCode);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link href="/app/links">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Links
          </Button>
        </Link>
      </div>

      {/* Link Info Card */}
      <Card>
        <CardHeader>
          <div className="flex items-start justify-between">
            <div className="space-y-2 flex-1">
              <div className="flex items-center gap-3">
                <CardTitle className="text-2xl">{link.title || "Untitled Link"}</CardTitle>
                <Badge variant={link.isActive ? "success" : "secondary"}>
                  {link.isActive ? "Active" : "Inactive"}
                </Badge>
              </div>
              <CardDescription>{link.description}</CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button variant="outline" size="sm" onClick={handleCopyLink}>
                <Copy className="h-4 w-4 mr-2" />
                Copy
              </Button>
              <Link href={`/app/links/${link.id}/edit`}>
                <Button variant="outline" size="sm">
                  <Edit className="h-4 w-4 mr-2" />
                  Edit
                </Button>
              </Link>
              <Button
                variant="destructive"
                size="sm"
                onClick={handleDeleteLink}
                disabled={deleteMutation.isPending}
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Short URL */}
          <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/50">
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Short URL</p>
              <code className="text-lg font-mono text-primary font-semibold">
                {shortUrl}
              </code>
            </div>
            <div className="flex items-center gap-2">
              <a href={shortUrl} target="_blank" rel="noopener noreferrer">
                <Button variant="ghost" size="sm">
                  <ExternalLink className="h-4 w-4" />
                </Button>
              </a>
              {/* QR Code Dialog */}
              <Dialog>
                <DialogTrigger asChild>
                  <Button variant="ghost" size="sm">
                    <QrCode className="h-4 w-4" />
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>QR Code</DialogTitle>
                    <DialogDescription>
                      Scan this QR code to open the short link
                    </DialogDescription>
                  </DialogHeader>
                  <div className="flex flex-col items-center gap-4 py-6">
                    <QRCodeSVG value={shortUrl} size={256} level="H" />
                    <p className="text-sm text-muted-foreground">{shortUrl}</p>
                  </div>
                </DialogContent>
              </Dialog>
            </div>
          </div>

          {/* Original URL */}
          <div className="space-y-1">
            <p className="text-xs text-muted-foreground">Original URL</p>
            <a
              href={link.originalUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-sm hover:text-primary flex items-center gap-2 break-all"
            >
              {link.originalUrl}
              <ExternalLink className="h-3 w-3 flex-shrink-0" />
            </a>
          </div>

          {/* Metadata */}
          <div className="flex flex-wrap gap-4 text-sm">
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">Created:</span>
              <span>{formatDate(link.createdAt)}</span>
            </div>
            {link.expiresAt && (
              <div className="flex items-center gap-2">
                <Calendar className="h-4 w-4 text-muted-foreground" />
                <span className="text-muted-foreground">Expires:</span>
                <span>{formatDate(link.expiresAt)}</span>
              </div>
            )}
            {link.tags && link.tags.length > 0 && (
              <div className="flex items-center gap-2">
                <Tag className="h-4 w-4 text-muted-foreground" />
                {link.tags.map((tag) => (
                  <Badge key={tag} variant="secondary">
                    {tag}
                  </Badge>
                ))}
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Stats Overview */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Clicks</CardTitle>
            <MousePointerClick className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.totalClicks || 0)}
            </div>
            <p className="text-xs text-muted-foreground">All-time clicks</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Unique Visitors</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {formatNumber(stats?.uniqueVisitors || 0)}
            </div>
            <p className="text-xs text-muted-foreground">Unique visitors</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Top Country</CardTitle>
            <Globe className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {stats?.clicksByCountry?.[0]?.country || "N/A"}
            </div>
            <p className="text-xs text-muted-foreground">
              {stats?.clicksByCountry?.[0]?.percentage.toFixed(1) || 0}% of clicks
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Top Device</CardTitle>
            <Smartphone className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {stats?.clicksByDevice?.[0]?.deviceType || "N/A"}
            </div>
            <p className="text-xs text-muted-foreground">
              {stats?.clicksByDevice?.[0]?.percentage.toFixed(1) || 0}% of clicks
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Analytics Tabs */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5" />
                Analytics
              </CardTitle>
              <CardDescription>
                Detailed insights into link performance
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExportData("csv")}
              >
                <Download className="h-4 w-4 mr-2" />
                Export CSV
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleExportData("json")}
              >
                <Download className="h-4 w-4 mr-2" />
                Export JSON
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {statsLoading ? (
            <div className="h-64 flex items-center justify-center">
              <p className="text-muted-foreground">Loading analytics...</p>
            </div>
          ) : !stats || stats.totalClicks === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center">
              <MousePointerClick className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No click data yet</p>
              <p className="text-sm text-muted-foreground">Share your link to start tracking clicks</p>
            </div>
          ) : (
            <Tabs defaultValue="overview" className="space-y-4">
              <TabsList>
                <TabsTrigger value="overview">Overview</TabsTrigger>
                <TabsTrigger value="devices">Devices</TabsTrigger>
                <TabsTrigger value="locations">Locations</TabsTrigger>
                <TabsTrigger value="referrers">Referrers</TabsTrigger>
              </TabsList>

              <TabsContent value="overview" className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold mb-4">Click Trends</h3>
                  <ClickChart data={stats.clicksByDate || []} variant="area" />
                </div>
              </TabsContent>

              <TabsContent value="devices" className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold mb-4">Device Distribution</h3>
                  <DeviceChart data={stats.clicksByDevice || []} />
                </div>
              </TabsContent>

              <TabsContent value="locations" className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold mb-4">Top Countries</h3>
                  <LocationChart data={stats.clicksByCountry || []} />
                </div>
              </TabsContent>

              <TabsContent value="referrers" className="space-y-4">
                <div>
                  <h3 className="text-lg font-semibold mb-4">Traffic Sources</h3>
                  <ReferrerTable data={stats.clicksByReferrer || []} />
                </div>
              </TabsContent>
            </Tabs>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
