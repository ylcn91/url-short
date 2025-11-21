/**
 * API Client for URL Shortener Backend
 * Handles all HTTP requests with authentication, error handling, and type safety
 */

import {
  AuthResponse,
  LoginRequest,
  SignupRequest,
  ShortLink,
  CreateLinkRequest,
  UpdateLinkRequest,
  BulkCreateRequest,
  LinkStats,
  ApiResponse,
  PaginatedResponse,
  ErrorResponse,
  DashboardStats,
  Workspace,
  ApiKey,
} from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";

class ApiError extends Error {
  constructor(
    public status: number,
    public message: string,
    public details?: any
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * Base fetch wrapper with authentication and error handling
 * Automatically unwraps ApiResponse<T> wrapper from backend
 */
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("auth_token") : null;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    // Handle different status codes
    if (response.status === 204) {
      return {} as T;
    }

    const data = await response.json();

    if (!response.ok) {
      const error = data as ErrorResponse;
      throw new ApiError(
        response.status,
        error.message || "An error occurred",
        error
      );
    }

    // Unwrap ApiResponse<T> wrapper from backend
    // Backend returns: { success: true, data: T, message: string }
    // We extract and return just the data
    if (data && typeof data === 'object' && 'success' in data && 'data' in data) {
      return data.data as T;
    }

    return data;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }

    // Network or parsing errors
    throw new ApiError(
      0,
      error instanceof Error ? error.message : "Network error occurred"
    );
  }
}

// ===========================
// Authentication API
// ===========================

export const authApi = {
  /**
   * Login with email and password
   */
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    });
  },

  /**
   * Register a new user
   */
  signup: async (data: SignupRequest): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>("/auth/signup", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  /**
   * Get current user profile
   */
  me: async (): Promise<AuthResponse> => {
    return fetchApi<AuthResponse>("/auth/me");
  },

  /**
   * Logout (client-side only, clears token)
   */
  logout: () => {
    if (typeof window !== "undefined") {
      localStorage.removeItem("auth_token");
    }
  },
};

// ===========================
// Short Links API
// ===========================

export const linksApi = {
  /**
   * Get all links for a workspace with optional filtering
   */
  getLinks: async (params?: {
    workspaceId?: number;
    page?: number;
    pageSize?: number;
    search?: string;
    tags?: string[];
    status?: "active" | "inactive" | "expired";
  }): Promise<PaginatedResponse<ShortLink>> => {
    const queryParams = new URLSearchParams();
    if (params?.workspaceId) queryParams.set("workspaceId", params.workspaceId.toString());
    if (params?.page) queryParams.set("page", params.page.toString());
    if (params?.pageSize) queryParams.set("pageSize", params.pageSize.toString());
    if (params?.search) queryParams.set("search", params.search);
    if (params?.status) queryParams.set("status", params.status);
    if (params?.tags?.length) queryParams.set("tags", params.tags.join(","));

    const query = queryParams.toString();
    return fetchApi<PaginatedResponse<ShortLink>>(
      `/links${query ? `?${query}` : ""}`
    );
  },

  /**
   * Get a single link by ID
   */
  getLink: async (id: number): Promise<ShortLink> => {
    return fetchApi<ShortLink>(`/links/${id}`);
  },

  /**
   * Create a new short link
   */
  createLink: async (data: CreateLinkRequest): Promise<ShortLink> => {
    return fetchApi<ShortLink>("/links", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  /**
   * Update an existing link
   */
  updateLink: async (id: number, data: UpdateLinkRequest): Promise<ShortLink> => {
    return fetchApi<ShortLink>(`/links/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  /**
   * Delete a link
   */
  deleteLink: async (id: number): Promise<void> => {
    return fetchApi<void>(`/links/${id}`, {
      method: "DELETE",
    });
  },

  /**
   * Bulk create links
   */
  bulkCreate: async (data: BulkCreateRequest): Promise<ShortLink[]> => {
    return fetchApi<ShortLink[]>("/links/bulk", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  /**
   * Toggle link active status
   */
  toggleActive: async (id: number): Promise<ShortLink> => {
    return fetchApi<ShortLink>(`/links/${id}/toggle`, {
      method: "PATCH",
    });
  },
};

// ===========================
// Analytics API
// ===========================

export const analyticsApi = {
  /**
   * Get analytics for a specific link
   */
  getLinkStats: async (
    linkId: number,
    params?: {
      startDate?: string;
      endDate?: string;
    }
  ): Promise<LinkStats> => {
    const queryParams = new URLSearchParams();
    if (params?.startDate) queryParams.set("startDate", params.startDate);
    if (params?.endDate) queryParams.set("endDate", params.endDate);

    const query = queryParams.toString();
    return fetchApi<LinkStats>(
      `/analytics/links/${linkId}${query ? `?${query}` : ""}`
    );
  },

  /**
   * Get dashboard overview stats
   */
  getDashboardStats: async (workspaceId: number): Promise<DashboardStats> => {
    return fetchApi<DashboardStats>(`/analytics/dashboard/${workspaceId}`);
  },

  /**
   * Export analytics data
   */
  exportData: async (
    linkId: number,
    format: "csv" | "json"
  ): Promise<Blob> => {
    const token = typeof window !== "undefined" ? localStorage.getItem("auth_token") : null;
    const response = await fetch(
      `${API_BASE_URL}/analytics/links/${linkId}/export?format=${format}`,
      {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      }
    );

    if (!response.ok) {
      throw new ApiError(response.status, "Export failed");
    }

    return response.blob();
  },
};

// ===========================
// Workspace API
// ===========================

export const workspaceApi = {
  /**
   * Get workspace details
   */
  getWorkspace: async (id: number): Promise<Workspace> => {
    return fetchApi<Workspace>(`/workspaces/${id}`);
  },

  /**
   * Update workspace settings
   */
  updateWorkspace: async (
    id: number,
    data: Partial<Workspace>
  ): Promise<Workspace> => {
    return fetchApi<Workspace>(`/workspaces/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  /**
   * Get workspace API keys
   */
  getApiKeys: async (workspaceId: number): Promise<ApiKey[]> => {
    return fetchApi<ApiKey[]>(`/workspaces/${workspaceId}/api-keys`);
  },

  /**
   * Create a new API key
   */
  createApiKey: async (
    workspaceId: number,
    name: string
  ): Promise<ApiKey> => {
    return fetchApi<ApiKey>(`/workspaces/${workspaceId}/api-keys`, {
      method: "POST",
      body: JSON.stringify({ name }),
    });
  },

  /**
   * Delete an API key
   */
  deleteApiKey: async (workspaceId: number, keyId: number): Promise<void> => {
    return fetchApi<void>(`/workspaces/${workspaceId}/api-keys/${keyId}`, {
      method: "DELETE",
    });
  },
};

export { ApiError };
