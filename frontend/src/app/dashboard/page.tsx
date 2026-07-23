"use client";

import { BellOutlined, BookOutlined, RiseOutlined, ShopOutlined, StarOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Empty, Select, Spin, Statistic, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import Link from "next/link";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { markets, type DashboardProduct, userWorkflowApi } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import { useMarketStore } from "@/store/market-store";
import styles from "../workspace.module.css";

export default function DashboardPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const market = useMarketStore((state) => state.selectedMarket);
  const setMarket = useMarketStore((state) => state.setSelectedMarket);
  const dashboard = useQuery({ queryKey: ["dashboard", market], queryFn: () => userWorkflowApi.dashboard(accessToken, market), enabled: Boolean(accessToken) });
  const columns: TableColumnsType<DashboardProduct> = [
    { title: "商品", dataIndex: "title", render: (title, row) => <Link href={`/products/${row.productId}`}>{title}</Link> },
    { title: "价格", width: 130, align: "right", render: (_, row) => `${row.currency} ${Number(row.currentPrice).toFixed(2)}` },
    { title: "7日销量增长", width: 130, align: "right", render: (_, row) => row.salesGrowthRate7d == null ? "-" : `${Number(row.salesGrowthRate7d).toFixed(2)}%` },
    { title: "综合评分", width: 110, align: "right", render: (_, row) => row.selectionScore?.toFixed(2) ?? "-" },
  ];
  return <ProtectedRoute><div className={styles.page}><AppHeader /><main className={styles.main}>
    <section className={styles.heading}><div><p className={styles.eyebrow}>Dashboard</p><h1>选品工作台</h1><p>查看当前市场机会、个人收藏与待处理提醒。</p></div><Select aria-label="切换市场" value={market} onChange={setMarket} options={markets.map((value) => ({ value, label: value }))} /></section>
    {dashboard.isLoading && <Spin size="large" />} {dashboard.isError && <Alert type="error" showIcon message="工作台加载失败" description={dashboard.error.message} />}
    {dashboard.data && <>
      <section className={styles.metricGrid} aria-label="工作台指标">
        <Statistic title="当前市场商品" value={dashboard.data.productCount} prefix={<ShopOutlined />} />
        <Statistic title="近7日新增" value={dashboard.data.newProducts7d} prefix={<RiseOutlined />} />
        <Statistic title="推荐商品" value={dashboard.data.recommendedProducts} prefix={<StarOutlined />} />
        <Statistic title="选品库" value={dashboard.data.watchlistProductCount} prefix={<BookOutlined />} />
        <Statistic title="未读提醒" value={dashboard.data.unreadAlertCount} prefix={<BellOutlined />} />
      </section>
      <section className={styles.tableBand}><div className={styles.bandTitle}><h2>近7日销量增长最高</h2><Tag>{dashboard.data.market}</Tag></div><Table rowKey="productId" columns={columns} dataSource={dashboard.data.topSalesGrowthProducts} pagination={false} locale={{ emptyText: <Empty description="暂无可展示的分析结果" /> }} /></section>
      <section className={styles.tableBand}><div className={styles.bandTitle}><h2>综合评分最高</h2><Tag>{dashboard.data.market}</Tag></div><Table rowKey="productId" columns={columns} dataSource={dashboard.data.topSelectionScoreProducts} pagination={false} locale={{ emptyText: <Empty description="暂无可展示的分析结果" /> }} /></section>
    </>}
  </main></div></ProtectedRoute>;
}
