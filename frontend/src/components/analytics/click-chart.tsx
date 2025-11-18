"use client";

import * as React from "react";
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import { format } from "date-fns";
import type { DateClickData } from "@/lib/types";

interface ClickChartProps {
  data: DateClickData[];
  variant?: "line" | "area";
}

/**
 * Click Chart Component
 * Displays click trends over time using Recharts
 * Supports line and area chart variants
 */
export function ClickChart({ data, variant = "area" }: ClickChartProps) {
  // Transform data for chart
  const chartData = data.map((item) => ({
    date: format(new Date(item.date), "MMM dd"),
    clicks: item.clicks,
    unique: item.uniqueVisitors,
  }));

  const Chart = variant === "area" ? AreaChart : LineChart;
  const DataComponent = variant === "area" ? Area : Line;

  return (
    <ResponsiveContainer width="100%" height={300}>
      <Chart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          dataKey="date"
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <YAxis
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "hsl(var(--background))",
            border: "1px solid hsl(var(--border))",
            borderRadius: "6px",
          }}
        />
        <Legend />
        {variant === "area" ? (
          <>
            <Area
              type="monotone"
              dataKey="clicks"
              stroke="hsl(var(--primary))"
              fill="hsl(var(--primary))"
              fillOpacity={0.2}
              name="Total Clicks"
            />
            <Area
              type="monotone"
              dataKey="unique"
              stroke="hsl(var(--chart-2))"
              fill="hsl(var(--chart-2))"
              fillOpacity={0.2}
              name="Unique Visitors"
            />
          </>
        ) : (
          <>
            <Line
              type="monotone"
              dataKey="clicks"
              stroke="hsl(var(--primary))"
              strokeWidth={2}
              name="Total Clicks"
            />
            <Line
              type="monotone"
              dataKey="unique"
              stroke="hsl(var(--chart-2))"
              strokeWidth={2}
              name="Unique Visitors"
            />
          </>
        )}
      </Chart>
    </ResponsiveContainer>
  );
}
