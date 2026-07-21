import { afterEach, describe, expect, it, vi } from "vitest";
import { authApi } from "./auth";

function response(status: number, data: unknown, code = 0, message = "success") {
  return new Response(JSON.stringify({
    code,
    message,
    data,
    requestId: "test-request-id",
    timestamp: "2026-07-21T08:30:00Z",
  }), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("authApi", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("sends login credentials with the refresh cookie enabled", async () => {
    const session = {
      accessToken: "access-token",
      tokenType: "Bearer",
      accessExpiresAt: "2026-07-21T10:30:00Z",
      user: { id: 1, email: "buyer@example.com", username: "Buyer", role: "USER" },
    };
    const fetchMock = vi.fn().mockResolvedValue(response(200, session));
    vi.stubGlobal("fetch", fetchMock);

    await expect(authApi.login({ email: "buyer@example.com", password: "Password1" }))
      .resolves.toEqual(session);
    expect(fetchMock).toHaveBeenCalledWith("/api/v1/auth/login", expect.objectContaining({
      method: "POST",
      credentials: "include",
      body: JSON.stringify({ email: "buyer@example.com", password: "Password1" }),
    }));
  });

  it("adds the bearer token to authenticated requests", async () => {
    const fetchMock = vi.fn().mockResolvedValue(response(200, {
      id: 1, email: "buyer@example.com", username: "Buyer", role: "USER",
    }));
    vi.stubGlobal("fetch", fetchMock);

    await authApi.getCurrentUser("access-token");

    expect(fetchMock).toHaveBeenCalledWith("/api/v1/auth/me", expect.objectContaining({
      credentials: "include",
      headers: expect.objectContaining({ Authorization: "Bearer access-token" }),
    }));
  });

  it("maps unified API failures to AuthApiError", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response(401, null, 40104, "用户名或密码错误")));

    await expect(authApi.login({ email: "buyer@example.com", password: "wrong" }))
      .rejects.toMatchObject({ code: 40104, status: 401, message: "用户名或密码错误" });
  });
});
