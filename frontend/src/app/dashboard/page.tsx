"use client";

import { CheckCircleFilled, ShopOutlined } from "@ant-design/icons";
import { Button } from "antd";
import Link from "next/link";
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
              <p>浏览已导入商品、切换市场并查看每日趋势。</p>
            </div>
            <div className={styles.status}><CheckCircleFilled /> 已认证</div>
          </section>
          <div style={{ marginTop: 24 }}><Link href="/products"><Button type="primary" icon={<ShopOutlined />}>打开商品榜单</Button></Link></div>
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
