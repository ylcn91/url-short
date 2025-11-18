"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Plus,
  Search,
  Filter,
  MoreVertical,
  Copy,
  ExternalLink,
  Edit,
  Trash2,
  BarChart3,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { linksApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import { formatNumber, formatDate, getShortUrl, copyToClipboard } from "@/lib/utils";
import { useToast } from "@/components/ui/use-toast";

/**
 * Links List Page
 * Displays all links with search, filtering, and pagination
 * Supports bulk actions and quick link management
 */
export default function LinksPage() {
  const router = useRouter();
  const workspace = useAuthStore((state) => state.workspace);
  const { toast } = useToast();

  const [search, setSearch] = React.useState("");
  const [status, setStatus] = React.useState<"all" | "active" | "inactive" | "expired">("all");
  const [page, setPage] = React.useState(1);
  const pageSize = 10;

  // Fetch links with filters
  const { data, isLoading, refetch } = useQuery({
    queryKey: ["links", workspace?.id, search, status, page],
    queryFn: () =>
      linksApi.getLinks({
        workspaceId: workspace!.id,
        page,
        pageSize,
        search: search || undefined,
        status: status === "all" ? undefined : status,
      }),
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

  const handleDeleteLink = async (id: string) => {
    if (!confirm("Are you sure you want to delete this link?")) return;

    try {
      await linksApi.deleteLink(id);
      toast({
        title: "Link deleted",
        description: "The link has been successfully deleted",
      });
      refetch();
    } catch (error: any) {
      toast({
        variant: "destructive",
        title: "Error",
        description: error.message || "Failed to delete link",
      });
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Links</h1>
          <p className="text-muted-foreground">
            Manage and track all your shortened links
          </p>
        </div>
        <Link href="/app/links/new">
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            Create Link
          </Button>
        </Link>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            {/* Search */}
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search links by URL or slug..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9"
              />
            </div>

            {/* Status Filter */}
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-muted-foreground" />
              <div className="flex gap-2">
                {["all", "active", "inactive", "expired"].map((s) => (
                  <Button
                    key={s}
                    variant={status === s ? "default" : "outline"}
                    size="sm"
                    onClick={() => setStatus(s as any)}
                  >
                    {s.charAt(0).toUpperCase() + s.slice(1)}
                  </Button>
                ))}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Links Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>All Links</CardTitle>
              <CardDescription>
                {data?.total || 0} total links
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-16 bg-muted animate-pulse rounded" />
              ))}
            </div>
          ) : !data?.items || data.items.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground mb-4">
                No links found. Create your first link to get started.
              </p>
              <Link href="/app/links/new">
                <Button>
                  <Plus className="mr-2 h-4 w-4" />
                  Create Link
                </Button>
              </Link>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Short Link</TableHead>
                    <TableHead>Original URL</TableHead>
                    <TableHead className="text-center">Clicks</TableHead>
                    <TableHead className="text-center">Status</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.items.map((link) => (
                    <TableRow key={link.id}>
                      <TableCell>
                        <div className="flex flex-col gap-1">
                          <code className="text-sm font-medium">
                            {getShortUrl(link.shortCode)}
                          </code>
                          {link.title && (
                            <span className="text-xs text-muted-foreground">
                              {link.title}
                            </span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="max-w-xs">
                        <a
                          href={link.originalUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="truncate hover:text-primary flex items-center gap-1"
                        >
                          {link.originalUrl}
                          <ExternalLink className="h-3 w-3" />
                        </a>
                      </TableCell>
                      <TableCell className="text-center font-medium">
                        {formatNumber(link.clickCount)}
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={
                            link.isActive
                              ? "success"
                              : link.expiresAt && new Date(link.expiresAt) < new Date()
                              ? "warning"
                              : "secondary"
                          }
                        >
                          {link.isActive
                            ? "Active"
                            : link.expiresAt && new Date(link.expiresAt) < new Date()
                            ? "Expired"
                            : "Inactive"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatDate(link.createdAt)}
                      </TableCell>
                      <TableCell className="text-right">
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="sm">
                              <MoreVertical className="h-4 w-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuLabel>Actions</DropdownMenuLabel>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => handleCopyLink(link.shortCode)}
                            >
                              <Copy className="mr-2 h-4 w-4" />
                              Copy Link
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => router.push(`/app/links/${link.id}`)}
                            >
                              <BarChart3 className="mr-2 h-4 w-4" />
                              View Analytics
                            </DropdownMenuItem>
                            <DropdownMenuItem
                              onClick={() => router.push(`/app/links/${link.id}/edit`)}
                            >
                              <Edit className="mr-2 h-4 w-4" />
                              Edit
                            </DropdownMenuItem>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem
                              onClick={() => handleDeleteLink(link.id)}
                              className="text-destructive"
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              Delete
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              {data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Showing {(page - 1) * pageSize + 1} to{" "}
                    {Math.min(page * pageSize, data.total)} of {data.total} links
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page - 1)}
                      disabled={page === 1}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <span className="text-sm">
                      Page {page} of {data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page + 1)}
                      disabled={page === data.totalPages}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
