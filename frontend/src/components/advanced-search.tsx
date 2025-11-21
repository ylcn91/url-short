"use client";

import * as React from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search, X, Filter } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

export interface SearchFilters {
  query: string;
  status: "all" | "active" | "inactive" | "expired";
  sortBy: "createdAt" | "clicks" | "title";
  sortOrder: "asc" | "desc";
  dateFrom?: string;
  dateTo?: string;
  tags?: string[];
}

interface AdvancedSearchProps {
  onSearch: (filters: SearchFilters) => void;
  initialFilters?: Partial<SearchFilters>;
}

export function AdvancedSearch({ onSearch, initialFilters }: AdvancedSearchProps) {
  const [filters, setFilters] = React.useState<SearchFilters>({
    query: initialFilters?.query || "",
    status: initialFilters?.status || "all",
    sortBy: initialFilters?.sortBy || "createdAt",
    sortOrder: initialFilters?.sortOrder || "desc",
    dateFrom: initialFilters?.dateFrom,
    dateTo: initialFilters?.dateTo,
  });

  const [isAdvancedOpen, setIsAdvancedOpen] = React.useState(false);

  const handleSearch = () => {
    onSearch(filters);
    setIsAdvancedOpen(false);
  };

  const handleReset = () => {
    const resetFilters: SearchFilters = {
      query: "",
      status: "all",
      sortBy: "createdAt",
      sortOrder: "desc",
    };
    setFilters(resetFilters);
    onSearch(resetFilters);
  };

  const activeFilterCount = React.useMemo(() => {
    let count = 0;
    if (filters.query) count++;
    if (filters.status !== "all") count++;
    if (filters.dateFrom) count++;
    if (filters.dateTo) count++;
    return count;
  }, [filters]);

  return (
    <div className="flex gap-2">
      <div className="flex-1 relative">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search by URL, short code, or tags..."
          className="pl-10 pr-10"
          value={filters.query}
          onChange={(e) => setFilters({ ...filters, query: e.target.value })}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              handleSearch();
            }
          }}
        />
        {filters.query && (
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-1 top-1/2 -translate-y-1/2 h-7 w-7"
            onClick={() => setFilters({ ...filters, query: "" })}
          >
            <X className="h-4 w-4" />
          </Button>
        )}
      </div>

      <Dialog open={isAdvancedOpen} onOpenChange={setIsAdvancedOpen}>
        <DialogTrigger asChild>
          <Button variant="outline" className="gap-2">
            <Filter className="h-4 w-4" />
            Filters
            {activeFilterCount > 0 && (
              <span className="ml-1 rounded-full bg-primary px-2 py-0.5 text-xs text-primary-foreground">
                {activeFilterCount}
              </span>
            )}
          </Button>
        </DialogTrigger>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Advanced Search & Filters</DialogTitle>
            <DialogDescription>
              Refine your search with advanced filtering options
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-6">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label>Status</Label>
                <Select
                  value={filters.status}
                  onValueChange={(value: any) => setFilters({ ...filters, status: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Links</SelectItem>
                    <SelectItem value="active">Active Only</SelectItem>
                    <SelectItem value="inactive">Inactive Only</SelectItem>
                    <SelectItem value="expired">Expired Only</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Sort By</Label>
                <div className="flex gap-2">
                  <Select
                    value={filters.sortBy}
                    onValueChange={(value: any) => setFilters({ ...filters, sortBy: value })}
                  >
                    <SelectTrigger className="flex-1">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="createdAt">Created Date</SelectItem>
                      <SelectItem value="clicks">Click Count</SelectItem>
                      <SelectItem value="title">Title</SelectItem>
                    </SelectContent>
                  </Select>
                  <Select
                    value={filters.sortOrder}
                    onValueChange={(value: any) => setFilters({ ...filters, sortOrder: value })}
                  >
                    <SelectTrigger className="w-32">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="desc">Descending</SelectItem>
                      <SelectItem value="asc">Ascending</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="dateFrom">Date From</Label>
                <Input
                  id="dateFrom"
                  type="date"
                  value={filters.dateFrom || ""}
                  onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="dateTo">Date To</Label>
                <Input
                  id="dateTo"
                  type="date"
                  value={filters.dateTo || ""}
                  onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                />
              </div>
            </div>

            <div className="flex gap-2 justify-end">
              <Button variant="outline" onClick={handleReset}>
                Reset
              </Button>
              <Button onClick={handleSearch}>
                Apply Filters
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Button onClick={handleSearch}>Search</Button>
    </div>
  );
}
