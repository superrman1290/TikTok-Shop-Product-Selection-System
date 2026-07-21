import { beforeEach, describe, expect, it } from "vitest";
import { useAuthStore } from "./auth-store";

const session = {
  accessToken: "access-token",
  tokenType: "Bearer" as const,
  accessExpiresAt: "2026-07-21T10:30:00Z",
  user: { id: 1, email: "buyer@example.com", username: "Buyer", role: "USER" as const },
};

describe("auth store", () => {
  beforeEach(() => {
    useAuthStore.setState({ accessToken: null, user: null, initialized: false });
  });

  it("stores an authenticated session in memory", () => {
    useAuthStore.getState().setSession(session);
    expect(useAuthStore.getState()).toMatchObject({
      accessToken: "access-token",
      user: session.user,
      initialized: true,
    });
  });

  it("clears tokens and marks bootstrap complete", () => {
    useAuthStore.getState().setSession(session);
    useAuthStore.getState().clearSession();
    expect(useAuthStore.getState()).toMatchObject({ accessToken: null, user: null, initialized: true });
  });
});
