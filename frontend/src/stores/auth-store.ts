/**
 * Global authentication state management with Zustand
 * Handles user session, workspace selection, and auth actions
 */

import { create } from "zustand";
import { User, Workspace, AuthResponse } from "@/lib/types";
import { authStorage } from "@/lib/auth";
import { authApi } from "@/lib/api";

interface AuthState {
  user: User | null;
  workspace: Workspace | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  setAuth: (data: AuthResponse) => void;
  clearAuth: () => void;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, name: string, workspaceName: string, workspaceSlug: string) => Promise<void>;
  logout: () => void;
  loadAuthFromStorage: () => void;
  setWorkspace: (workspace: Workspace) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  workspace: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,

  setAuth: (data: AuthResponse) => {
    authStorage.saveAuth(data);
    set({
      user: data.user,
      workspace: data.workspace,
      token: data.accessToken,
      isAuthenticated: true,
      error: null,
    });
  },

  clearAuth: () => {
    authStorage.clearAuth();
    set({
      user: null,
      workspace: null,
      token: null,
      isAuthenticated: false,
      error: null,
    });
  },

  login: async (email: string, password: string) => {
    set({ isLoading: true, error: null });
    try {
      const data = await authApi.login({ email, password });
      authStorage.saveAuth(data);
      set({
        user: data.user,
        workspace: data.workspace,
        token: data.accessToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error: any) {
      set({
        isLoading: false,
        error: error.message || "Login failed",
      });
      throw error;
    }
  },

  signup: async (email: string, password: string, name: string, workspaceName: string, workspaceSlug: string) => {
    set({ isLoading: true, error: null });
    try {
      const data = await authApi.signup({ email, password, name, workspaceName, workspaceSlug });
      authStorage.saveAuth(data);
      set({
        user: data.user,
        workspace: data.workspace,
        token: data.accessToken,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });
    } catch (error: any) {
      set({
        isLoading: false,
        error: error.message || "Signup failed",
      });
      throw error;
    }
  },

  logout: () => {
    authStorage.clearAuth();
    set({
      user: null,
      workspace: null,
      token: null,
      isAuthenticated: false,
      error: null,
    });
  },

  loadAuthFromStorage: () => {
    const token = authStorage.getToken();
    const user = authStorage.getUser();
    const workspace = authStorage.getWorkspace();

    if (token && user && workspace) {
      set({
        token,
        user,
        workspace,
        isAuthenticated: true,
      });
    }
  },

  setWorkspace: (workspace: Workspace) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("auth_workspace", JSON.stringify(workspace));
    }
    set({ workspace });
  },
}));
