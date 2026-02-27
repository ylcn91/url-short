"use client";

import * as React from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Separator } from "@/components/ui/separator";
import { PageHeader } from "@/components/layouts/page-header";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UserPlus, Shield, Eye, Edit, Mail } from "lucide-react";
import { useAuthStore } from "@/stores/auth-store";

function getInitials(name: string): string {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
}

/**
 * Team Management Page
 * Shows workspace members and allows inviting new ones
 */
export default function TeamPage() {
  const user = useAuthStore((state) => state.user);
  const [inviteOpen, setInviteOpen] = React.useState(false);
  const [inviteEmail, setInviteEmail] = React.useState("");
  const [inviteRole, setInviteRole] = React.useState("MEMBER");

  const members = user
    ? [
        {
          id: user.id,
          fullName: user.fullName,
          email: user.email,
          role: user.role,
          joinedAt: user.createdAt,
          isCurrentUser: true,
        },
      ]
    : [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Team"
        description="Manage your workspace members and their roles"
      >
        <Dialog open={inviteOpen} onOpenChange={setInviteOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <UserPlus className="h-4 w-4" />
              Invite Member
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Invite Team Member</DialogTitle>
              <DialogDescription>
                Send an invitation to join your workspace
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="invite-email">Email Address</Label>
                <Input
                  id="invite-email"
                  type="email"
                  placeholder="colleague@example.com"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="invite-role">Role</Label>
                <Select value={inviteRole} onValueChange={setInviteRole}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select a role" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ADMIN">Admin</SelectItem>
                    <SelectItem value="MEMBER">Member</SelectItem>
                    <SelectItem value="VIEWER">Viewer</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setInviteOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={() => {
                  setInviteOpen(false);
                  setInviteEmail("");
                  setInviteRole("MEMBER");
                }}
                disabled={!inviteEmail.trim()}
              >
                <Mail className="mr-2 h-4 w-4" />
                Send Invite
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </PageHeader>

      {/* Members Grid */}
      <div>
        <h2 className="text-lg font-semibold mb-1">Members</h2>
        <p className="text-sm text-muted-foreground mb-4">
          People who have access to this workspace
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {members.map((member) => (
            <Card key={member.id}>
              <CardContent className="flex items-center gap-4 p-6">
                <Avatar className="h-12 w-12">
                  <AvatarFallback className="text-sm font-medium">
                    {getInitials(member.fullName)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="font-medium truncate">{member.fullName}</p>
                    {member.isCurrentUser && (
                      <Badge variant="outline" className="text-xs shrink-0">
                        You
                      </Badge>
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground truncate">
                    {member.email}
                  </p>
                  <Badge
                    variant={member.role === "ADMIN" ? "default" : "secondary"}
                    className="mt-2 gap-1"
                  >
                    {member.role === "ADMIN" && <Shield className="h-3 w-3" />}
                    {member.role === "MEMBER" && <Edit className="h-3 w-3" />}
                    {member.role === "VIEWER" && <Eye className="h-3 w-3" />}
                    {member.role}
                  </Badge>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      <Separator />

      {/* Roles Explanation */}
      <div>
        <h2 className="text-lg font-semibold mb-1">Roles</h2>
        <p className="text-sm text-muted-foreground mb-4">
          Understanding workspace permissions
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                <Shield className="h-5 w-5 text-foreground" />
              </div>
              <CardTitle className="mt-3 text-base">Admin</CardTitle>
              <CardDescription>
                Full access to all workspace settings, members, and links
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                <Edit className="h-5 w-5 text-muted-foreground" />
              </div>
              <CardTitle className="mt-3 text-base">Member</CardTitle>
              <CardDescription>
                Can create, edit, and delete links. Cannot manage workspace
                settings
              </CardDescription>
            </CardHeader>
          </Card>

          <Card>
            <CardHeader>
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted">
                <Eye className="h-5 w-5 text-muted-foreground" />
              </div>
              <CardTitle className="mt-3 text-base">Viewer</CardTitle>
              <CardDescription>
                Read-only access to links and analytics
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>
    </div>
  );
}
