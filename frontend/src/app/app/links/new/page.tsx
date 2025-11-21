"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useMutation } from "@tanstack/react-query";
import { ArrowLeft, Loader2, Link2, Calendar, Tag, Lock } from "lucide-react";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/components/ui/use-toast";
import { linksApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import type { CreateLinkRequest } from "@/lib/types";

// Form validation schema
const createLinkSchema = z.object({
  originalUrl: z
    .string()
    .url("Please enter a valid URL")
    .min(1, "URL is required"),
  customSlug: z
    .string()
    .optional()
    .refine(
      (val) => !val || /^[a-zA-Z0-9-_]+$/.test(val),
      "Slug can only contain letters, numbers, hyphens, and underscores"
    ),
  title: z.string().optional(),
  description: z.string().optional(),
  tags: z.string().optional(),
  expiresAt: z.string().optional(),
  useCustomSlug: z.boolean().default(false),
  setExpiration: z.boolean().default(false),
});

type CreateLinkFormData = z.infer<typeof createLinkSchema>;

/**
 * Create Link Page
 * Form for creating a new short link with advanced options
 * Includes URL validation, custom slugs, expiration, and tags
 */
export default function CreateLinkPage() {
  const router = useRouter();
  const { toast } = useToast();
  const workspace = useAuthStore((state) => state.workspace);

  const {
    register,
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<CreateLinkFormData>({
    resolver: zodResolver(createLinkSchema),
    defaultValues: {
      useCustomSlug: false,
      setExpiration: false,
    },
  });

  const useCustomSlug = watch("useCustomSlug");
  const setExpiration = watch("setExpiration");

  // Create link mutation
  const createLinkMutation = useMutation({
    mutationFn: (data: CreateLinkRequest) => linksApi.createLink(data),
    onSuccess: (link) => {
      toast({
        title: "Link created!",
        description: `Your short link is ready: ${link.shortCode}`,
      });
      router.push(`/app/links/${link.id}`);
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to create link",
        description: error.message || "Please try again",
      });
    },
  });

  const onSubmit = (data: CreateLinkFormData) => {
    if (!workspace) return;

    const requestData: CreateLinkRequest = {
      originalUrl: data.originalUrl,
      workspaceId: workspace.id,
      title: data.title || undefined,
      description: data.description || undefined,
      customSlug: data.useCustomSlug ? data.customSlug : undefined,
      tags: data.tags ? data.tags.split(",").map((t) => t.trim()) : undefined,
      expiresAt: data.setExpiration ? data.expiresAt : undefined,
    };

    createLinkMutation.mutate(requestData);
  };

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

      <div>
        <h1 className="text-3xl font-bold tracking-tight">Create Short Link</h1>
        <p className="text-muted-foreground">
          Generate a short link with custom options and tracking
        </p>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Information */}
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Link2 className="h-5 w-5" />
                  Basic Information
                </CardTitle>
                <CardDescription>
                  Enter the URL you want to shorten
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* Original URL */}
                <div className="space-y-2">
                  <Label htmlFor="originalUrl">
                    Original URL <span className="text-destructive">*</span>
                  </Label>
                  <Input
                    id="originalUrl"
                    type="url"
                    placeholder="https://example.com/very/long/url"
                    {...register("originalUrl")}
                    aria-invalid={errors.originalUrl ? "true" : "false"}
                  />
                  {errors.originalUrl && (
                    <p className="text-sm text-destructive">
                      {errors.originalUrl.message}
                    </p>
                  )}
                </div>

                {/* Title */}
                <div className="space-y-2">
                  <Label htmlFor="title">Title (optional)</Label>
                  <Input
                    id="title"
                    placeholder="Marketing Campaign Q1"
                    {...register("title")}
                  />
                  <p className="text-xs text-muted-foreground">
                    A friendly name to help you identify this link
                  </p>
                </div>

                {/* Description */}
                <div className="space-y-2">
                  <Label htmlFor="description">Description (optional)</Label>
                  <Textarea
                    id="description"
                    placeholder="Add notes about this link..."
                    {...register("description")}
                    rows={3}
                  />
                </div>
              </CardContent>
            </Card>

            {/* Advanced Options */}
            <Card>
              <CardHeader>
                <CardTitle>Advanced Options</CardTitle>
                <CardDescription>
                  Customize your short link behavior
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {/* Custom Slug */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="space-y-0.5">
                      <Label htmlFor="useCustomSlug" className="text-base">
                        Custom Slug
                      </Label>
                      <p className="text-sm text-muted-foreground">
                        Choose a memorable short code
                      </p>
                    </div>
                    <Controller
                      name="useCustomSlug"
                      control={control}
                      render={({ field }) => (
                        <Switch
                          id="useCustomSlug"
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      )}
                    />
                  </div>
                  {useCustomSlug && (
                    <div className="space-y-2 pl-0">
                      <Label htmlFor="customSlug">Short Code</Label>
                      <Input
                        id="customSlug"
                        placeholder="my-campaign"
                        {...register("customSlug")}
                        aria-invalid={errors.customSlug ? "true" : "false"}
                      />
                      {errors.customSlug && (
                        <p className="text-sm text-destructive">
                          {errors.customSlug.message}
                        </p>
                      )}
                      <p className="text-xs text-muted-foreground">
                        Letters, numbers, hyphens, and underscores only
                      </p>
                    </div>
                  )}
                </div>

                {/* Expiration */}
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="space-y-0.5">
                      <Label htmlFor="setExpiration" className="text-base flex items-center gap-2">
                        <Calendar className="h-4 w-4" />
                        Link Expiration
                      </Label>
                      <p className="text-sm text-muted-foreground">
                        Set when this link should expire
                      </p>
                    </div>
                    <Controller
                      name="setExpiration"
                      control={control}
                      render={({ field }) => (
                        <Switch
                          id="setExpiration"
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      )}
                    />
                  </div>
                  {setExpiration && (
                    <div className="space-y-2 pl-0">
                      <Label htmlFor="expiresAt">Expiration Date & Time</Label>
                      <Input
                        id="expiresAt"
                        type="datetime-local"
                        {...register("expiresAt")}
                        min={new Date().toISOString().slice(0, 16)}
                      />
                      <p className="text-xs text-muted-foreground">
                        Link will stop working after this date
                      </p>
                    </div>
                  )}
                </div>

                {/* Tags */}
                <div className="space-y-2">
                  <Label htmlFor="tags" className="flex items-center gap-2">
                    <Tag className="h-4 w-4" />
                    Tags (optional)
                  </Label>
                  <Input
                    id="tags"
                    placeholder="marketing, campaign, social"
                    {...register("tags")}
                  />
                  <p className="text-xs text-muted-foreground">
                    Comma-separated tags for organizing your links
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Preview & Actions */}
          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Preview</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="rounded-lg border bg-muted/50 p-4 space-y-3">
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">
                      Short URL
                    </p>
                    <code className="text-sm font-mono text-primary">
                      {useCustomSlug && watch("customSlug")
                        ? `short.link/${watch("customSlug")}`
                        : "short.link/abc123"}
                    </code>
                  </div>
                  <div className="pt-3 border-t">
                    <p className="text-xs text-muted-foreground mb-1">
                      Redirects to
                    </p>
                    <p className="text-xs truncate">
                      {watch("originalUrl") || "https://example.com/..."}
                    </p>
                  </div>
                </div>

                {watch("title") && (
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">Title</p>
                    <p className="text-sm font-medium">{watch("title")}</p>
                  </div>
                )}

                {watch("tags") && (
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">Tags</p>
                    <div className="flex flex-wrap gap-1">
                      {watch("tags")
                        ?.split(",")
                        .map((tag) => (
                          <span
                            key={tag}
                            className="text-xs px-2 py-1 rounded-full bg-primary/10 text-primary"
                          >
                            {tag.trim()}
                          </span>
                        ))}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6 space-y-3">
                <Button
                  type="submit"
                  className="w-full"
                  disabled={createLinkMutation.isPending}
                >
                  {createLinkMutation.isPending ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating...
                    </>
                  ) : (
                    "Create Short Link"
                  )}
                </Button>
                <Link href="/app/links" className="block">
                  <Button variant="outline" className="w-full">
                    Cancel
                  </Button>
                </Link>
              </CardContent>
            </Card>
          </div>
        </div>
      </form>
    </div>
  );
}
