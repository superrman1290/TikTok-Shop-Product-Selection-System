import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AuthShell } from "./auth-shell";

describe("AuthShell", () => {
  it("renders the authentication heading and form content", () => {
    render(
      <AuthShell title="登录工作台" subtitle="使用你的账号继续" footer={<a href="/register">创建账号</a>}>
        <button type="button">登录</button>
      </AuthShell>,
    );

    expect(screen.getByRole("heading", { name: "登录工作台" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "登录" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "创建账号" })).toHaveAttribute("href", "/register");
  });
});
