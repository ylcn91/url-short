"use client";

import * as React from "react";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import type { CountryClickData } from "@/lib/types";

interface LocationChartProps {
  data: CountryClickData[];
}

/**
 * Location Chart Component
 * Bar chart showing click distribution by country
 */
export function LocationChart({ data }: LocationChartProps) {
  // Take top 10 countries
  const chartData = data
    .slice(0, 10)
    .map((item) => ({
      country: item.country,
      clicks: item.clicks,
      percentage: item.percentage,
    }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={chartData} layout="vertical">
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          type="number"
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <YAxis
          dataKey="country"
          type="category"
          width={80}
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "hsl(var(--background))",
            border: "1px solid hsl(var(--border))",
            borderRadius: "6px",
          }}
          formatter={(value: number, name: string, props: any) => [
            `${value} clicks (${props.payload.percentage.toFixed(1)}%)`,
            "Clicks",
          ]}
        />
        <Bar dataKey="clicks" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
