"use client";

import { LockOutlined } from "@ant-design/icons";
import { Result, Spin } from "antd";
import { useRouter } from "next/navigation";
import type { PropsWithChildren } from "react";
import { useEffect } from "react";
import type { UserRole } from "@/lib/auth";
import { useAuthStore } from "@/store/auth-store";
import styles from "./protected-route.module.css";

interface ProtectedRouteProps extends PropsWithChildren {
  requiredRole?: UserRole;
}

export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const router = useRouter();
  const initialized = useAuthStore((state) => state.initialized);
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    if (initialized && !user) {
      router.replace("/login");
    }
  }, [initialized, router, user]);

  if (!initialized || !user) {
    return (
      <div className={styles.centered} aria-live="polite">
        <Spin size="large" />
        <span>正在验证登录状态</span>
      </div>
    );
  }

  if (requiredRole && user.role !== requiredRole) {
    return (
      <div className={styles.centered}>
        <Result
          icon={<LockOutlined />}
          status="403"
          title="无权访问"
          subTitle="当前账号没有此页面的访问权限。"
        />
      </div>
    );
  }

  return children;
}
