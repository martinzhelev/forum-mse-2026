import type {
  LoginResponse,
  MaintenanceStatus,
  Post,
  Reply,
  User,
  UserInput
} from "./types";

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ApiError(
      response.status,
      body?.message ?? body?.error ?? `${response.status} ${response.statusText}`
    );
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const api = {
  login: (username: string, password: string) =>
    request<LoginResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    }),
  listPosts: () => request<Post[]>("/posts"),
  getPost: (id: number) => request<Post>(`/posts/${id}`),
  createPost: (title: string, content: string, token: string) =>
    request<Post>("/posts", { method: "POST", body: JSON.stringify({ title, content }) }, token),
  listReplies: (postId: number) => request<Reply[]>(`/posts/${postId}/replies`),
  getReply: (id: number) => request<Reply>(`/replies/${id}`),
  createReply: (postId: number, content: string, token: string) =>
    request<Reply>(
      `/posts/${postId}/replies`,
      { method: "POST", body: JSON.stringify({ content }) },
      token
    ),
  listUsers: (token: string) => request<User[]>("/users", {}, token),
  getUser: (id: number, token: string) => request<User>(`/users/${id}`, {}, token),
  createUser: (input: UserInput, token: string) =>
    request<User>("/users", { method: "POST", body: JSON.stringify(input) }, token),
  updateUser: (id: number, input: UserInput, token: string) =>
    request<User>(`/users/${id}`, { method: "PUT", body: JSON.stringify(input) }, token),
  deleteUser: (id: number, token: string) =>
    request<void>(`/users/${id}`, { method: "DELETE" }, token),
  maintenanceStatus: (token: string) =>
    request<MaintenanceStatus>("/admin/maintenance/status", {}, token),
  startRestore: (estimatedDurationSeconds: number | null, token: string) =>
    request<MaintenanceStatus>(
      "/admin/maintenance/restore/start",
      {
        method: "POST",
        body: estimatedDurationSeconds
          ? JSON.stringify({ estimatedDurationSeconds })
          : undefined
      },
      token
    ),
  finishRestore: (token: string) =>
    request<MaintenanceStatus>("/admin/maintenance/restore/finish", { method: "POST" }, token),
  health: () => request<{ status: string }>("/actuator/health")
};
