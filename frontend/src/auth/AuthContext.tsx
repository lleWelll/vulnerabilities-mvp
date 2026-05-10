import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { AUTH_STORAGE_KEY, setUnauthorizedHandler } from "../api/apiClient";
import { authApi } from "../api/endpoints";
import type { AuthState, AuthUser, LoginRequest, RegisterRequest, RegisterResponse } from "../types";

interface AuthContextValue {
  auth: AuthState | null;
  user: AuthUser | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (payload: LoginRequest, remember?: boolean) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<RegisterResponse>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(() => readAuthState());

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    setAuth(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  useEffect(() => {
    if (!auth) {
      return undefined;
    }
    const delay = Math.max(auth.expiresAt - Date.now(), 0);
    const timeout = window.setTimeout(logout, delay);
    return () => window.clearTimeout(timeout);
  }, [auth, logout]);

  const login = useCallback(async (payload: LoginRequest, remember = true) => {
    const response = await authApi.login({
      username: payload.username.trim(),
      password: payload.password
    });
    const decoded = decodeJwtPayload(response.accessToken);
    const expiresAt = decoded?.exp ? decoded.exp * 1000 : Date.now() + response.expiresInSeconds * 1000;
    const nextAuth: AuthState = {
      token: response.accessToken,
      tokenType: response.tokenType || "Bearer",
      expiresAt,
      user: {
        username: decoded?.sub || payload.username.trim(),
        role: response.role,
        userId: typeof decoded?.uid === "number" ? decoded.uid : undefined
      }
    };
    const storage = remember ? localStorage : sessionStorage;
    localStorage.removeItem(AUTH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    storage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuth));
    setAuth(nextAuth);
  }, []);

  const register = useCallback(async (payload: RegisterRequest) => {
    return authApi.register({
      username: payload.username.trim(),
      password: payload.password
    });
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    auth,
    user: auth?.user || null,
    token: auth?.token || null,
    isAuthenticated: Boolean(auth),
    login,
    register,
    logout
  }), [auth, login, logout, register]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}

function readAuthState(): AuthState | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY) || sessionStorage.getItem(AUTH_STORAGE_KEY);
  if (!raw) {
    return null;
  }
  try {
    const parsed = JSON.parse(raw) as AuthState;
    if (!parsed.token || !parsed.user?.role || !parsed.expiresAt || Date.now() >= parsed.expiresAt) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

function decodeJwtPayload(token: string): { sub?: string; exp?: number; uid?: number } | null {
  const [, payload] = token.split(".");
  if (!payload) {
    return null;
  }
  try {
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
    return JSON.parse(window.atob(padded)) as { sub?: string; exp?: number; uid?: number };
  } catch {
    return null;
  }
}
