"use client";

import { AppstoreOutlined, ImportOutlined, LogoutOutlined, SettingOutlined, ShopOutlined, UserOutlined } from "@ant-design/icons";
import { Button } from "antd";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/auth";
import { useAuthStore } from "@/store/auth-store";
import styles from "./app-header.module.css";

export function AppHeader() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      clearSession();
      router.replace("/login");
    }
  };

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link className={styles.brand} href={user?.role === "ADMIN" ? "/admin" : "/dashboard"}>
          <span>TI</span>
          <strong>TikTok Product Insight</strong>
        </Link>
        <nav className={styles.nav} aria-label="账号导航">
          <Link href="/products"><ShopOutlined /> 商品</Link>
          {user?.role === "ADMIN" && <Link href="/admin/products"><AppstoreOutlined /> 商品管理</Link>}
          {user?.role === "ADMIN" && <Link href="/admin/imports"><ImportOutlined /> 导入</Link>}
          {user?.role === "ADMIN" && <Link href="/admin"><SettingOutlined /> 管理入口</Link>}
          <Link href="/profile"><UserOutlined /> 账号</Link>
          <Button type="text" icon={<LogoutOutlined />} onClick={logout}>退出</Button>
        </nav>
      </div>
    </header>
  );
}
