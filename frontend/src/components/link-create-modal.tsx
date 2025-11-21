"use client";

import * as React from "react";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UTMBuilder } from "@/components/utm-builder";
import { useToast } from "@/components/ui/use-toast";
import { linksApi } from "@/lib/api";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Link2, Calendar, Tag, Lock, Zap } from "lucide-react";

interface LinkCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  workspaceId?: number;
}

export function LinkCreateModal({ open, onOpenChange, workspaceId = 1 }: LinkCreateModalProps) {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const [currentTab, setCurrentTab] = React.useState("basic");

  const [formData, setFormData] = React.useState({
    originalUrl: "",
    customSlug: "",
    title: "",
    description: "",
    tags: "",
    expiresAt: "",
    maxClicks: "",
    password: "",
    enablePassword: false,
  });

  const createMutation = useMutation({
    mutationFn: async (data: any) => {
      const payload: any = {
        originalUrl: data.originalUrl,
      };

      if (data.customSlug) payload.customSlug = data.customSlug;
      if (data.title) payload.title = data.title;
      if (data.description) payload.description = data.description;
      if (data.tags) payload.tags = data.tags.split(",").map((t: string) => t.trim());
      if (data.expiresAt) payload.expiresAt = new Date(data.expiresAt).toISOString();
      if (data.maxClicks) payload.maxClicks = parseInt(data.maxClicks);
      if (data.enablePassword && data.password) payload.password = data.password;

      return linksApi.createLink(payload);
    },
    onSuccess: (data) => {
      toast({
        title: "Link Created!",
        description: `Short URL: ${data.shortUrl}`,
      });
      queryClient.invalidateQueries({ queryKey: ["links"] });
      handleClose();
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Error",
        description: error.message || "Failed to create link",
      });
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  const handleClose = () => {
    setFormData({
      originalUrl: "",
      customSlug: "",
      title: "",
      description: "",
      tags: "",
      expiresAt: "",
      maxClicks: "",
      password: "",
      enablePassword: false,
    });
    setCurrentTab("basic");
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Create Short Link</DialogTitle>
          <DialogDescription>
            Shorten your URL and customize it with advanced options
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <Tabs value={currentTab} onValueChange={setCurrentTab}>
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="basic">
                <Link2 className="h-4 w-4 mr-2" />
                Basic
              </TabsTrigger>
              <TabsTrigger value="utm">
                <Zap className="h-4 w-4 mr-2" />
                UTM
              </TabsTrigger>
              <TabsTrigger value="expiry">
                <Calendar className="h-4 w-4 mr-2" />
                Expiry
              </TabsTrigger>
              <TabsTrigger value="security">
                <Lock className="h-4 w-4 mr-2" />
                Security
              </TabsTrigger>
            </TabsList>

            <TabsContent value="basic" className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="originalUrl">Destination URL *</Label>
                <Input
                  id="originalUrl"
                  type="url"
                  placeholder="https://example.com/your-long-url"
                  value={formData.originalUrl}
                  onChange={(e) => setFormData({ ...formData, originalUrl: e.target.value })}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="customSlug">Custom Back-Half (Optional)</Label>
                <Input
                  id="customSlug"
                  placeholder="my-custom-link"
                  value={formData.customSlug}
                  onChange={(e) => setFormData({ ...formData, customSlug: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Leave empty for auto-generated short code
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="title">Title (Optional)</Label>
                <Input
                  id="title"
                  placeholder="Marketing Campaign 2024"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description (Optional)</Label>
                <Textarea
                  id="description"
                  placeholder="Add a description for this link..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="tags">Tags (Optional)</Label>
                <Input
                  id="tags"
                  placeholder="marketing, campaign, social"
                  value={formData.tags}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Separate tags with commas
                </p>
              </div>
            </TabsContent>

            <TabsContent value="utm" className="mt-4">
              <UTMBuilder
                baseUrl={formData.originalUrl}
                onUrlChange={(url) => setFormData({ ...formData, originalUrl: url })}
              />
            </TabsContent>

            <TabsContent value="expiry" className="space-y-4 mt-4">
              <div className="space-y-2">
                <Label htmlFor="expiresAt">Expiration Date</Label>
                <Input
                  id="expiresAt"
                  type="datetime-local"
                  value={formData.expiresAt}
                  onChange={(e) => setFormData({ ...formData, expiresAt: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Link will stop working after this date
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="maxClicks">Max Clicks</Label>
                <Input
                  id="maxClicks"
                  type="number"
                  min="1"
                  placeholder="1000"
                  value={formData.maxClicks}
                  onChange={(e) => setFormData({ ...formData, maxClicks: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">
                  Link will stop working after this many clicks
                </p>
              </div>
            </TabsContent>

            <TabsContent value="security" className="space-y-4 mt-4">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>Password Protection</Label>
                  <p className="text-sm text-muted-foreground">
                    Require a password to access this link
                  </p>
                </div>
                <Switch
                  checked={formData.enablePassword}
                  onCheckedChange={(checked) =>
                    setFormData({ ...formData, enablePassword: checked })
                  }
                />
              </div>

              {formData.enablePassword && (
                <div className="space-y-2">
                  <Label htmlFor="password">Password</Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="Enter password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  />
                </div>
              )}
            </TabsContent>
          </Tabs>

          <div className="flex gap-2 justify-end mt-6 pt-4 border-t">
            <Button type="button" variant="outline" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Creating..." : "Create Link"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
