/**
 * Authentication utilities
 * Handles token management and auth state persistence
 */

import { AuthResponse } from "./types";

const TOKEN_KEY = "auth_token";
const USER_KEY = "auth_user";
const WORKSPACE_KEY = "auth_workspace";

export const authStorage = {
  /**
   * Save authentication data to localStorage
   */
  saveAuth: (data: AuthResponse): void => {
    if (typeof window === "undefined") return;

    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USER_KEY, JSON.stringify(data.user));
    localStorage.setItem(WORKSPACE_KEY, JSON.stringify(data.workspace));
  },

  /**
   * Get stored token
   */
  getToken: (): string | null => {
    if (typeof window === "undefined") return null;
    return localStorage.getItem(TOKEN_KEY);
  },

  /**
   * Get stored user data
   */
  getUser: () => {
    if (typeof window === "undefined") return null;
    const user = localStorage.getItem(USER_KEY);
    return user ? JSON.parse(user) : null;
  },

  /**
   * Get stored workspace data
   */
  getWorkspace: () => {
    if (typeof window === "undefined") return null;
    const workspace = localStorage.getItem(WORKSPACE_KEY);
    return workspace ? JSON.parse(workspace) : null;
  },

  /**
   * Clear all authentication data
   */
  clearAuth: (): void => {
    if (typeof window === "undefined") return;

    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(WORKSPACE_KEY);
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated: (): boolean => {
    return !!authStorage.getToken();
  },
};

/**
 * Decode JWT token (simple, without verification)
 * Note: This is just for reading claims, not for validation
 */
export function decodeToken(token: string): any {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    return null;
  }
}

/**
 * Check if token is expired
 */
export function isTokenExpired(token: string): boolean {
  const decoded = decodeToken(token);
  if (!decoded || !decoded.exp) return true;

  const currentTime = Date.now() / 1000;
  return decoded.exp < currentTime;
}
