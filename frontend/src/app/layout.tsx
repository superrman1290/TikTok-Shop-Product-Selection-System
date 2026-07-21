import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Providers } from "./providers";
import "antd/dist/reset.css";
import "./globals.css";

export const metadata: Metadata = {
  title: "TikTok Product Insight",
  description: "TikTok Shop product selection operations console",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
