"use client";

import { DatabaseOutlined, FileSearchOutlined, ImportOutlined, ProductOutlined, SafetyCertificateFilled, SettingOutlined, TeamOutlined } from "@ant-design/icons";
import { Button } from "antd";
import { Statistic } from "antd";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { useAuthStore } from "@/store/auth-store";
import { adminApi } from "@/lib/products";
import styles from "../workspace.module.css";

export default function AdminPage() {
  const user = useAuthStore((state) => state.user);
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const dashboard = useQuery({ queryKey: ["admin-dashboard"], queryFn: () => adminApi.dashboard(accessToken), enabled: Boolean(accessToken) });

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
          {dashboard.data && <section className={styles.metricGrid} aria-label="管理员仪表盘指标">
            <Statistic title="用户总数" value={dashboard.data.userCount} />
            <Statistic title="启用用户" value={dashboard.data.enabledUserCount} />
            <Statistic title="商品总数" value={dashboard.data.productCount} />
            <Statistic title="近 7 日导入" value={dashboard.data.importJobs7d} />
            <Statistic title="导入成功率" value={dashboard.data.importSuccessRate} suffix="%" />
            <Statistic title="待执行分析" value={dashboard.data.pendingAnalysisJobs} />
            <Statistic title="失败分析" value={dashboard.data.failedAnalysisJobs} />
            <Statistic title="近 7 日提醒" value={dashboard.data.alerts7d} />
          </section>}
          <section className={styles.details} aria-label="管理员账号">
            <div className={styles.detail}><span>管理员</span><strong>{user?.username}</strong></div>
            <div className={styles.detail}><span>邮箱</span><strong>{user?.email}</strong></div>
            <div className={styles.detail}><span>权限边界</span><strong>Verified</strong></div>
          </section>
          <div style={{ display: "flex", gap: 12, marginTop: 24, flexWrap: "wrap" }}>
            <Link href="/admin/products"><Button icon={<ProductOutlined />}>商品管理</Button></Link>
            <Link href="/admin/imports"><Button type="primary" icon={<ImportOutlined />}>CSV 导入</Button></Link>
            <Link href="/admin/users"><Button icon={<TeamOutlined />}>用户管理</Button></Link>
            <Link href="/admin/data-sources"><Button icon={<DatabaseOutlined />}>数据源</Button></Link>
            <Link href="/admin/jobs"><Button icon={<SettingOutlined />}>分析任务</Button></Link>
            <Link href="/admin/audit-logs"><Button icon={<FileSearchOutlined />}>审计日志</Button></Link>
          </div>
        </main>
      </div>
    </ProtectedRoute>
  );
}
