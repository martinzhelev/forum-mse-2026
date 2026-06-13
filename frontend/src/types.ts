export type UserRole = "ADMIN" | "MODERATOR" | "USER";

export interface Post {
  id: number;
  title: string;
  content: string;
  createdAt: string;
}

export interface Reply {
  id: number;
  postId: number;
  content: string;
  createdAt: string;
}

export interface User {
  id: number;
  username: string;
  email?: string | null;
  role: UserRole;
  createdAt: string;
}

export interface UserInput {
  username: string;
  email?: string | null;
  role: UserRole;
  password?: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface Session {
  token: string;
  userId: number;
  username: string;
  role: UserRole;
  expiresAt: number;
}

export interface MaintenanceStatus {
  restoreInProgress: boolean;
  retryAfterSeconds: number;
  restoreStartedAt?: string | null;
  estimatedCompletionAt?: string | null;
}
