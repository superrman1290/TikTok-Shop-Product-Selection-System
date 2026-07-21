"use client";

import { SafetyCertificateFilled } from "@ant-design/icons";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { useAuthStore } from "@/store/auth-store";
import styles from "../workspace.module.css";

export default function AdminPage() {
  const user = useAuthStore((state) => state.user);

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className={styles.page}>
        <AppHeader />
        <main className={styles.main}>
          <section className={styles.heading}>
            <div>
              <p className={styles.eyebrow}>Administration</p>
              <h1>管理员权限已验证</h1>
              <p>管理业务模块将在其规定阶段接入。</p>
            </div>
            <div className={styles.status}><SafetyCertificateFilled /> ADMIN</div>
          </section>
          <section className={styles.details} aria-label="管理员账号">
            <div className={styles.detail}><span>管理员</span><strong>{user?.username}</strong></div>
            <div className={styles.detail}><span>邮箱</span><strong>{user?.email}</strong></div>
            <div className={styles.detail}><span>权限边界</span><strong>Verified</strong></div>
          </section>
        </main>
      </div>
    </ProtectedRoute>
  );
}
