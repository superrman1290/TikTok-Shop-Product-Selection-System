export type ComponentStatus = "UP" | "DOWN";

export interface SystemHealth {
  application: ComponentStatus;
  database: ComponentStatus;
  redis: ComponentStatus;
  storage: ComponentStatus;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  requestId: string;
  timestamp: string;
}

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

export async function fetchSystemHealth(signal?: AbortSignal): Promise<ApiResponse<SystemHealth>> {
  const response = await fetch(`${apiBaseUrl}/api/v1/health`, {
    method: "GET",
    headers: { Accept: "application/json" },
    cache: "no-store",
    signal,
  });

  if (!response.ok) {
    throw new Error(`Health check failed with status ${response.status}`);
  }

  return response.json() as Promise<ApiResponse<SystemHealth>>;
}
