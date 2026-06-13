import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { api } from "./api";
import type { Session, UserRole } from "./types";

const STORAGE_KEY = "forum-session";

function readSession(): Session | null {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "null") as Session | null;
    if (!session || session.expiresAt <= Date.now()) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

function decodeToken(token: string) {
  const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  return JSON.parse(atob(payload)) as { uid: number; sub: string; role: UserRole; exp: number };
}

interface AuthValue {
  session: Session | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readSession);

  const value = useMemo<AuthValue>(
    () => ({
      session,
      login: async (username, password) => {
        const response = await api.login(username, password);
        const claims = decodeToken(response.accessToken);
        const next = {
          token: response.accessToken,
          userId: claims.uid,
          username: claims.sub,
          role: claims.role,
          expiresAt: claims.exp * 1000
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        setSession(next);
      },
      logout: () => {
        localStorage.removeItem(STORAGE_KEY);
        setSession(null);
      }
    }),
    [session]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// The hook intentionally lives with its provider so session logic stays in one module.
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used inside AuthProvider");
  return value;
}
