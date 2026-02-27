"use client";

import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Save,
  Key,
  Plus,
  Trash2,
  Copy,
  Loader2,
  AlertTriangle,
} from "lucide-react";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Separator } from "@/components/ui/separator";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useToast } from "@/components/ui/use-toast";
import { workspaceApi } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import { formatDate, copyToClipboard } from "@/lib/utils";

// Form validation schema
const workspaceSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
  customDomain: z.string().optional(),
  allowCustomSlugs: z.boolean(),
  requireAuthentication: z.boolean(),
});

type WorkspaceFormData = z.infer<typeof workspaceSchema>;

function getPlanBadgeVariant(plan: string): "secondary" | "default" | "success" | "warning" {
  switch (plan) {
    case "FREE":
      return "secondary";
    case "PRO":
      return "default";
    case "TEAM":
      return "success";
    case "ENTERPRISE":
      return "warning";
    default:
      return "secondary";
  }
}

/**
 * Workspace Settings Page
 * Manage workspace configuration, API keys, and team settings
 */
export default function WorkspaceSettingsPage() {
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const workspace = useAuthStore((state) => state.workspace);
  const updateWorkspaceState = useAuthStore((state) => state.setWorkspace);

  const [newKeyName, setNewKeyName] = React.useState("");
  const [showNewKeyDialog, setShowNewKeyDialog] = React.useState(false);
  const [newlyCreatedKey, setNewlyCreatedKey] = React.useState<string | null>(null);

  // Fetch workspace details
  const { data: workspaceData, isLoading } = useQuery({
    queryKey: ["workspace", workspace?.id],
    queryFn: () => workspaceApi.getWorkspace(workspace!.id),
    enabled: !!workspace,
  });

  // Fetch API keys
  const { data: apiKeys, refetch: refetchKeys } = useQuery({
    queryKey: ["api-keys", workspace?.id],
    queryFn: () => workspaceApi.getApiKeys(workspace!.id),
    enabled: !!workspace,
  });

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<WorkspaceFormData>({
    resolver: zodResolver(workspaceSchema),
    values: workspaceData
      ? {
          name: workspaceData.name,
          customDomain: workspaceData.settings.customDomain || "",
          allowCustomSlugs: workspaceData.settings.allowCustomSlugs,
          requireAuthentication: workspaceData.settings.requireAuthentication,
        }
      : undefined,
  });

  // Update workspace mutation
  const updateWorkspaceMutation = useMutation({
    mutationFn: (data: Partial<WorkspaceFormData>) =>
      workspaceApi.updateWorkspace(workspace!.id, {
        name: data.name,
        settings: {
          customDomain: data.customDomain,
          allowCustomSlugs: data.allowCustomSlugs || false,
          requireAuthentication: data.requireAuthentication || false,
        },
      }),
    onSuccess: (updatedWorkspace) => {
      toast({
        title: "Settings saved",
        description: "Workspace settings have been updated successfully",
      });
      updateWorkspaceState(updatedWorkspace);
      queryClient.invalidateQueries({ queryKey: ["workspace", workspace?.id] });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to save settings",
        description: error.message || "Please try again",
      });
    },
  });

  // Create API key mutation
  const createKeyMutation = useMutation({
    mutationFn: (name: string) => workspaceApi.createApiKey(workspace!.id, name),
    onSuccess: (apiKey) => {
      setNewlyCreatedKey(apiKey.key);
      setNewKeyName("");
      refetchKeys();
      toast({
        title: "API key created",
        description: "Save this key securely - you won't be able to see it again!",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to create API key",
        description: error.message || "Please try again",
      });
    },
  });

  // Delete API key mutation
  const deleteKeyMutation = useMutation({
    mutationFn: (keyId: number) => workspaceApi.deleteApiKey(workspace!.id, keyId),
    onSuccess: () => {
      refetchKeys();
      toast({
        title: "API key deleted",
        description: "The API key has been revoked",
      });
    },
    onError: (error: any) => {
      toast({
        variant: "destructive",
        title: "Failed to delete API key",
        description: error.message || "Please try again",
      });
    },
  });

  const onSubmit = (data: WorkspaceFormData) => {
    updateWorkspaceMutation.mutate(data);
  };

  const handleCreateKey = () => {
    if (!newKeyName.trim()) {
      toast({
        variant: "destructive",
        title: "Key name required",
        description: "Please enter a name for the API key",
      });
      return;
    }
    createKeyMutation.mutate(newKeyName);
  };

  const handleDeleteKey = (keyId: number) => {
    if (!confirm("Are you sure you want to delete this API key? Applications using this key will stop working.")) {
      return;
    }
    deleteKeyMutation.mutate(keyId);
  };

  const handleCopyKey = async (key: string) => {
    const success = await copyToClipboard(key);
    if (success) {
      toast({
        title: "Copied!",
        description: "API key copied to clipboard",
      });
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Workspace Settings"
          description="Manage your workspace configuration and API access"
        />
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-32" />
            <Skeleton className="h-4 w-64 mt-2" />
          </CardHeader>
          <CardContent className="space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-1/2" />
          </CardContent>
        </Card>
        <Separator />
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-4 w-48 mt-2" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-32 w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Workspace Settings"
        description="Manage your workspace configuration and API access"
      />

      {/* General Settings */}
      <form onSubmit={handleSubmit(onSubmit)}>
        <Card>
          <CardHeader>
            <CardTitle>General Settings</CardTitle>
            <CardDescription>
              Basic workspace configuration and preferences
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Workspace Name */}
            <div className="space-y-2">
              <Label htmlFor="name">
                Workspace Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                {...register("name")}
                aria-invalid={errors.name ? "true" : "false"}
              />
              {errors.name && (
                <p className="text-sm text-destructive">{errors.name.message}</p>
              )}
            </div>

            {/* Plan Badge */}
            <div className="space-y-2">
              <Label>Current Plan</Label>
              <div>
                <Badge
                  variant={getPlanBadgeVariant(workspaceData?.plan || "FREE")}
                  className="text-base px-3 py-1"
                >
                  {workspaceData?.plan || "FREE"}
                </Badge>
              </div>
            </div>

            {/* Custom Domain */}
            <div className="space-y-2">
              <Label htmlFor="customDomain">Custom Domain</Label>
              <Input
                id="customDomain"
                placeholder="links.yourdomain.com"
                {...register("customDomain")}
              />
              <p className="text-xs text-muted-foreground">
                Use your own domain for short links (Pro plan and above)
              </p>
            </div>

            <Separator />

            {/* Allow Custom Slugs */}
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="allowCustomSlugs" className="text-base">
                  Allow Custom Slugs
                </Label>
                <p className="text-sm text-muted-foreground">
                  Let users create custom short codes for their links
                </p>
              </div>
              <Switch
                id="allowCustomSlugs"
                checked={watch("allowCustomSlugs")}
                onCheckedChange={(checked) => setValue("allowCustomSlugs", checked)}
              />
            </div>

            <Separator />

            {/* Require Authentication */}
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="requireAuthentication" className="text-base">
                  Require Authentication
                </Label>
                <p className="text-sm text-muted-foreground">
                  Require users to be logged in to access links
                </p>
              </div>
              <Switch
                id="requireAuthentication"
                checked={watch("requireAuthentication")}
                onCheckedChange={(checked) =>
                  setValue("requireAuthentication", checked)
                }
              />
            </div>

            {/* Save Button */}
            <div className="flex justify-end pt-4 border-t">
              <Button
                type="submit"
                disabled={updateWorkspaceMutation.isPending}
              >
                {updateWorkspaceMutation.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    Save Changes
                  </>
                )}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>

      <Separator />

      {/* API Keys */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="flex items-center gap-2">
                <Key className="h-5 w-5" />
                API Keys
              </CardTitle>
              <CardDescription>
                Manage API keys for programmatic access
              </CardDescription>
            </div>
            <Dialog open={showNewKeyDialog} onOpenChange={setShowNewKeyDialog}>
              <DialogTrigger asChild>
                <Button>
                  <Plus className="mr-2 h-4 w-4" />
                  Create API Key
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Create API Key</DialogTitle>
                  <DialogDescription>
                    Generate a new API key for accessing the API
                  </DialogDescription>
                </DialogHeader>
                {newlyCreatedKey ? (
                  <div className="space-y-4">
                    <div className="flex items-start gap-2 p-4 rounded-lg bg-yellow-500/10 border border-yellow-500/20">
                      <AlertTriangle className="h-5 w-5 text-yellow-500 mt-0.5" />
                      <div className="space-y-1">
                        <p className="text-sm font-medium">Save this key securely!</p>
                        <p className="text-xs text-muted-foreground">
                          You won&apos;t be able to see it again after closing this dialog.
                        </p>
                      </div>
                    </div>
                    <div className="space-y-2">
                      <Label>API Key</Label>
                      <div className="flex gap-2">
                        <Input
                          value={newlyCreatedKey}
                          readOnly
                          className="font-mono text-sm"
                        />
                        <Button
                          variant="outline"
                          onClick={() => handleCopyKey(newlyCreatedKey)}
                        >
                          <Copy className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="space-y-2">
                      <Label htmlFor="keyName">Key Name</Label>
                      <Input
                        id="keyName"
                        placeholder="Production Server"
                        value={newKeyName}
                        onChange={(e) => setNewKeyName(e.target.value)}
                      />
                      <p className="text-xs text-muted-foreground">
                        A descriptive name to identify this key
                      </p>
                    </div>
                  </div>
                )}
                <DialogFooter>
                  {newlyCreatedKey ? (
                    <Button
                      onClick={() => {
                        setNewlyCreatedKey(null);
                        setShowNewKeyDialog(false);
                      }}
                    >
                      Done
                    </Button>
                  ) : (
                    <>
                      <Button
                        variant="outline"
                        onClick={() => setShowNewKeyDialog(false)}
                      >
                        Cancel
                      </Button>
                      <Button
                        onClick={handleCreateKey}
                        disabled={createKeyMutation.isPending}
                      >
                        {createKeyMutation.isPending ? (
                          <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            Creating...
                          </>
                        ) : (
                          "Create Key"
                        )}
                      </Button>
                    </>
                  )}
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {!apiKeys || apiKeys.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <Key className="h-12 w-12 mx-auto mb-4 opacity-20" />
              <p>No API keys created yet</p>
              <p className="text-sm">Create an API key to access the API</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Key</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Last Used</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {apiKeys.map((key) => (
                  <TableRow key={key.id}>
                    <TableCell className="font-medium">{key.name}</TableCell>
                    <TableCell>
                      <code className="text-xs">
                        {key.key.substring(0, 20)}...
                      </code>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {formatDate(key.createdAt)}
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {key.lastUsedAt ? formatDate(key.lastUsedAt) : "Never"}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleDeleteKey(key.id)}
                        disabled={deleteKeyMutation.isPending}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
