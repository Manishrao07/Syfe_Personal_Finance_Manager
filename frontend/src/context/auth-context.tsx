"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { ApiError, authApi, categoryApi } from "@/lib/api";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

interface AuthContextValue {
  status: AuthStatus;
  username: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (fields: { username: string; password: string; fullName: string; phoneNumber: string }) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = "syfe-finance-username";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [username, setUsername] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    categoryApi
      .list()
      .then(() => {
        if (cancelled) return;
        setUsername(typeof window !== "undefined" ? window.localStorage.getItem(STORAGE_KEY) : null);
        setStatus("authenticated");
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          setStatus("unauthenticated");
        } else {
          // Network error / cold-start / CORS issue: treat as unauthenticated so the
          // login screen (with a clear error on next action) is shown rather than a
          // permanent spinner.
          setStatus("unauthenticated");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (usernameInput: string, password: string) => {
    await authApi.login({ username: usernameInput, password });
    if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, usernameInput);
    setUsername(usernameInput);
    setStatus("authenticated");
  }, []);

  const register = useCallback(
    async (fields: { username: string; password: string; fullName: string; phoneNumber: string }) => {
      await authApi.register(fields);
      await authApi.login({ username: fields.username, password: fields.password });
      if (typeof window !== "undefined") window.localStorage.setItem(STORAGE_KEY, fields.username);
      setUsername(fields.username);
      setStatus("authenticated");
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      if (typeof window !== "undefined") window.localStorage.removeItem(STORAGE_KEY);
      setUsername(null);
      setStatus("unauthenticated");
    }
  }, []);

  const value = useMemo(
    () => ({ status, username, login, register, logout }),
    [status, username, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
