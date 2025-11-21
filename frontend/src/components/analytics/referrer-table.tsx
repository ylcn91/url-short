"use client";

import * as React from "react";
import { ExternalLink } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import type { ReferrerClickData } from "@/lib/types";

interface ReferrerTableProps {
  data: ReferrerClickData[];
}

/**
 * Referrer Table Component
 * Displays top referrers with click counts and percentages
 */
export function ReferrerTable({ data }: ReferrerTableProps) {
  // Take top 10 referrers
  const topReferrers = data.slice(0, 10);

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Referrer</TableHead>
          <TableHead className="text-right">Clicks</TableHead>
          <TableHead className="text-right">Share</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {topReferrers.length === 0 ? (
          <TableRow>
            <TableCell colSpan={3} className="text-center text-muted-foreground">
              No referrer data available
            </TableCell>
          </TableRow>
        ) : (
          topReferrers.map((item, index) => (
            <TableRow key={index}>
              <TableCell>
                <div className="flex items-center gap-2">
                  <ExternalLink className="h-4 w-4 text-muted-foreground" />
                  <span className="font-medium truncate max-w-xs">
                    {item.referrer || "Direct / None"}
                  </span>
                </div>
              </TableCell>
              <TableCell className="text-right font-mono">
                {item.clicks.toLocaleString()}
              </TableCell>
              <TableCell className="text-right">
                <Badge variant="secondary">
                  {item.percentage.toFixed(1)}%
                </Badge>
              </TableCell>
            </TableRow>
          ))
        )}
      </TableBody>
    </Table>
  );
}
