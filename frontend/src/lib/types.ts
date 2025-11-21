/**
 * Type definitions for the URL Shortener application
 * These match the backend API response types
 */

export interface User {
  id: number;
  email: string;
  name: string;
  createdAt: string;
  role: "USER" | "ADMIN";
}

export interface Workspace {
  id: number;
  name: string;
  slug: string;
  plan: "FREE" | "PRO" | "TEAM" | "ENTERPRISE";
  ownerId: number;
  createdAt: string;
  settings: WorkspaceSettings;
}

export interface WorkspaceSettings {
  customDomain?: string;
  defaultExpiration?: number;
  allowCustomSlugs: boolean;
  requireAuthentication: boolean;
}

export interface ShortLink {
  id: number;
  shortCode: string;
  originalUrl: string;
  workspaceId: number;
  createdBy: number;
  title?: string;
  description?: string;
  tags: string[];
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
  isActive: boolean;
  clickCount: number;
  customSlug?: string;
  qrCodeUrl?: string;
}

export interface ClickEvent {
  id: number;
  shortLinkId: number;
  timestamp: string;
  ipAddress: string;
  userAgent: string;
  referer?: string;
  country?: string;
  city?: string;
  deviceType: "DESKTOP" | "MOBILE" | "TABLET" | "BOT";
  browser?: string;
  os?: string;
}

export interface LinkStats {
  totalClicks: number;
  uniqueVisitors: number;
  clicksByDate: DateClickData[];
  clicksByCountry: CountryClickData[];
  clicksByReferrer: ReferrerClickData[];
  clicksByDevice: DeviceClickData[];
  clicksByBrowser: BrowserClickData[];
}

export interface DateClickData {
  date: string;
  clicks: number;
  uniqueVisitors: number;
}

export interface CountryClickData {
  country: string;
  clicks: number;
  percentage: number;
}

export interface ReferrerClickData {
  referrer: string;
  clicks: number;
  percentage: number;
}

export interface DeviceClickData {
  deviceType: string;
  clicks: number;
  percentage: number;
}

export interface BrowserClickData {
  browser: string;
  clicks: number;
  percentage: number;
}

export interface ApiKey {
  id: number;
  name: string;
  key: string;
  workspaceId: number;
  createdAt: string;
  expiresAt?: string;
  lastUsedAt?: string;
}

export interface CreateLinkRequest {
  originalUrl: string;
  customSlug?: string;
  title?: string;
  description?: string;
  tags?: string[];
  expiresAt?: string;
  workspaceId: number;
}

export interface UpdateLinkRequest {
  title?: string;
  description?: string;
  tags?: string[];
  expiresAt?: string;
  isActive?: boolean;
}

export interface BulkCreateRequest {
  links: CreateLinkRequest[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  workspaceName: string;
  workspaceSlug: string;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
  workspace: Workspace;
}

export interface DashboardStats {
  totalLinks: number;
  totalClicks: number;
  activeLinks: number;
  clicksToday: number;
  clicksThisWeek: number;
  clicksThisMonth: number;
  topLinks: ShortLink[];
  recentActivity: ClickEvent[];
}

export interface PricingTier {
  name: string;
  price: number;
  interval: "month" | "year";
  features: string[];
  limits: {
    links: number | "unlimited";
    clicks: number | "unlimited";
    customDomain: boolean;
    analytics: "basic" | "advanced" | "enterprise";
    apiAccess: boolean;
    teamMembers: number | "unlimited";
  };
}
