"use client";

import { ImportOutlined, ProductOutlined, SafetyCertificateFilled } from "@ant-design/icons";
import { Button } from "antd";
import Link from "next/link";
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
              <h1>商品数据管理</h1>
              <p>维护全局商品数据并查看 CSV 导入结果。</p>
            </div>
            <div className={styles.status}><SafetyCertificateFilled /> ADMIN</div>
          </section>
          <section className={styles.details} aria-label="管理员账号">
            <div className={styles.detail}><span>管理员</span><strong>{user?.username}</strong></div>
            <div className={styles.detail}><span>邮箱</span><strong>{user?.email}</strong></div>
            <div className={styles.detail}><span>权限边界</span><strong>Verified</strong></div>
          </section>
          <div style={{ display: "flex", gap: 12, marginTop: 24, flexWrap: "wrap" }}>
            <Link href="/admin/products"><Button icon={<ProductOutlined />}>商品管理</Button></Link>
            <Link href="/admin/imports"><Button type="primary" icon={<ImportOutlined />}>CSV 导入</Button></Link>
          </div>
        </main>
      </div>
    </ProtectedRoute>
  );
}
