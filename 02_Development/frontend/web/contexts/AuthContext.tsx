"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import {
  auth,
  AuthTokensResponse,
  clearTokens,
  saveTokens,
  UserResponse,
} from "@/lib/api";

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (
    username: string,
    email: string,
    password: string
  ) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function persist(data: AuthTokensResponse) {
  saveTokens(data.accessToken, data.refreshToken);
  localStorage.setItem("user", JSON.stringify(data.user));
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const u = await auth.me();
      setUser(u);
      localStorage.setItem("user", JSON.stringify(u));
    } catch {
      setUser(null);
    }
  }, []);

  useEffect(() => {
    const cached = localStorage.getItem("user");
    const token = localStorage.getItem("accessToken");
    if (cached && token) {
      setUser(JSON.parse(cached));
      refreshUser().finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [refreshUser]);

  const login = useCallback(
    async (usernameOrEmail: string, password: string) => {
      const data = await auth.login({ usernameOrEmail, password });
      persist(data);
      setUser(data.user);
    },
    []
  );

  const register = useCallback(
    async (username: string, email: string, password: string) => {
      const data = await auth.register({ username, email, password });
      persist(data);
      setUser(data.user);
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      await auth.logout();
    } catch {}
    clearTokens();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{ user, loading, login, register, logout, refreshUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
