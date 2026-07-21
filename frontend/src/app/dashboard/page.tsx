"use client";

import { CheckCircleFilled } from "@ant-design/icons";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { useAuthStore } from "@/store/auth-store";
import styles from "../workspace.module.css";

export default function DashboardPage() {
  const user = useAuthStore((state) => state.user);

  return (
    <ProtectedRoute>
      <div className={styles.page}>
        <AppHeader />
        <main className={styles.main}>
          <section className={styles.heading}>
            <div>
              <p className={styles.eyebrow}>Workspace</p>
              <h1>欢迎回来，{user?.username}</h1>
              <p>你的账号已通过认证，业务工作区将在后续阶段接入。</p>
            </div>
            <div className={styles.status}><CheckCircleFilled /> 已认证</div>
          </section>
          <section className={styles.details} aria-label="当前账号">
            <div className={styles.detail}><span>邮箱</span><strong>{user?.email}</strong></div>
            <div className={styles.detail}><span>角色</span><strong>{user?.role}</strong></div>
            <div className={styles.detail}><span>会话</span><strong>Active</strong></div>
          </section>
        </main>
      </div>
    </ProtectedRoute>
  );
}
