export type UserRole = "USER" | "ADMIN";

export interface AuthUser {
  id: number;
  email: string;
  username: string;
  role: UserRole;
}

export interface AuthSession {
  accessToken: string;
  tokenType: "Bearer";
  accessExpiresAt: string;
  user: AuthUser;
}

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
  timestamp: string;
}

export class AuthApiError extends Error {
  constructor(
    message: string,
    public readonly code: number,
    public readonly status: number,
  ) {
    super(message);
    this.name = "AuthApiError";
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: "include",
    headers: {
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
  });
  const envelope = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || envelope.code !== 0) {
    throw new AuthApiError(envelope.message || "请求失败", envelope.code, response.status);
  }
  return envelope.data;
}

export const authApi = {
  register(input: { email: string; username: string; password: string }) {
    return request<AuthSession>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  login(input: { email: string; password: string }) {
    return request<AuthSession>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(input),
    });
  },

  refresh() {
    return request<AuthSession>("/api/v1/auth/refresh", { method: "POST" });
  },

  logout() {
    return request<void>("/api/v1/auth/logout", { method: "POST" });
  },

  getCurrentUser(accessToken: string) {
    return request<AuthUser>("/api/v1/auth/me", {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  },

  changePassword(accessToken: string, input: { currentPassword: string; newPassword: string }) {
    return request<void>("/api/v1/auth/password", {
      method: "PUT",
      headers: { Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify(input),
    });
  },
};
