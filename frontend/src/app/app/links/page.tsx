"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  Link2,
} from "lucide-react";
import { linksApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import { formatNumber, formatDate, getShortUrl, copyToClipboard } from "@/lib/utils";
import { useToast } from "@/components/ui/use-toast";

export default function LinksPage() {
  const router = useRouter();
  const workspace = useAuthStore((state) => state.workspace);
  const { toast } = useToast();

  const [search, setSearch] = React.useState("");
  const [debouncedSearch, setDebouncedSearch] = React.useState("");
  const [status, setStatus] = React.useState<"all" | "active" | "inactive" | "expired">("all");
  const [page, setPage] = React.useState(1);
  const pageSize = 10;

  React.useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ["links", workspace?.id, debouncedSearch, status, page],
    queryFn: () =>
      linksApi.getLinks({
        workspaceId: workspace!.id,
        page,
        pageSize,
        search: debouncedSearch || undefined,
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

  const handleDeleteLink = async (id: number) => {
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
      <PageHeader
        title="Links"
        description="Manage and track all your shortened links"
      >
        <Link href="/app/links/new">
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            Create Link
          </Button>
        </Link>
      </PageHeader>

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
              <div className="flex gap-1">
                {(["all", "active", "inactive", "expired"] as const).map((s) => (
                  <Button
                    key={s}
                    variant="ghost"
                    size="sm"
                    onClick={() => setStatus(s)}
                    className={
                      status === s
                        ? "bg-primary text-primary-foreground hover:bg-primary/90 hover:text-primary-foreground"
                        : ""
                    }
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
                {data?.totalElements || 0} total links
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-40" />
                    <Skeleton className="h-3 w-64" />
                  </div>
                  <Skeleton className="h-4 w-12" />
                  <Skeleton className="h-6 w-16 rounded-full" />
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-8 w-8" />
                </div>
              ))}
            </div>
          ) : !data?.content || data.content.length === 0 ? (
            <div className="text-center py-12">
              <Link2 className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
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
                  {data.content.map((link) => (
                    <TableRow key={link.id} className="hover:bg-muted/50 transition-colors">
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
                          <span
                            className={`inline-block h-2 w-2 rounded-full mr-1.5 ${
                              link.isActive
                                ? "bg-green-500"
                                : "bg-red-500"
                            }`}
                          />
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
                            <Button variant="ghost" size="sm" aria-label="Link actions">
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
                    {Math.min(page * pageSize, data.totalElements)} of {data.totalElements} links
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
