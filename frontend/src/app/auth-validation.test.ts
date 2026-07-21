import { describe, expect, it } from "vitest";
import { loginSchema, registerSchema } from "@/lib/auth-validation";

describe("authentication validation", () => {
  it("accepts valid login input and rejects malformed email", () => {
    expect(loginSchema.safeParse({ email: "buyer@example.com", password: "Password1" }).success).toBe(true);
    expect(loginSchema.safeParse({ email: "invalid", password: "Password1" }).success).toBe(false);
  });

  it("enforces password length, letter and digit rules", () => {
    expect(registerSchema.safeParse({
      email: "buyer@example.com", username: "Buyer", password: "Password1",
    }).success).toBe(true);
    expect(registerSchema.safeParse({
      email: "buyer@example.com", username: "Buyer", password: "lettersOnly",
    }).success).toBe(false);
    expect(registerSchema.safeParse({
      email: "buyer@example.com", username: "Buyer", password: "12345678",
    }).success).toBe(false);
  });
});
