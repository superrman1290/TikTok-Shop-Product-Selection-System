"use client";

import { AntdRegistry } from "@ant-design/nextjs-registry";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ConfigProvider } from "antd";
import type { PropsWithChildren } from "react";
import { useState } from "react";
import { AuthBootstrap } from "@/components/auth-bootstrap";

export function Providers({ children }: PropsWithChildren) {
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  }));

  return (
    <AntdRegistry>
      <ConfigProvider
        theme={{
          token: {
            colorPrimary: "#006d5b",
            colorInfo: "#2563eb",
            colorSuccess: "#177245",
            colorWarning: "#b45309",
            colorError: "#b42318",
            colorText: "#17202a",
            colorTextSecondary: "#59636e",
            colorBgLayout: "#f4f6f8",
            borderRadius: 6,
            fontFamily: "Inter, Segoe UI, Arial, sans-serif",
          },
          components: {
            Button: { controlHeight: 38 },
            Card: { borderRadiusLG: 6 },
          },
        }}
      >
        <QueryClientProvider client={queryClient}>
          <AuthBootstrap />
          {children}
        </QueryClientProvider>
      </ConfigProvider>
    </AntdRegistry>
  );
}
