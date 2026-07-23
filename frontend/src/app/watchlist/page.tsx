"use client";

import { DeleteOutlined, SettingOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import Link from "next/link";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { markets, type WatchlistItem, userWorkflowApi } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../product-workspace.module.css";

export default function WatchlistPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? ""; const client = useQueryClient(); const [market, setMarket] = useState<string>();
  const list = useQuery({ queryKey: ["watchlist", market], queryFn: () => userWorkflowApi.watchlist(accessToken, { page: 1, pageSize: 100, market }), enabled: Boolean(accessToken) });
  const remove = useMutation({ mutationFn: (productId: number) => userWorkflowApi.removeWatchlist(accessToken, productId), onSuccess: () => client.invalidateQueries({ queryKey: ["watchlist"] }) });
  const columns: TableColumnsType<WatchlistItem> = [
    { title: "商品", dataIndex: "title", render: (title, row) => <Link href={`/products/${row.productId}`}>{title}</Link> },
    { title: "市场", dataIndex: "market", width: 84, render: (value) => <Tag>{value}</Tag> },
    { title: "当前价格", width: 130, align: "right", render: (_, row) => `${row.currency} ${Number(row.currentPrice).toFixed(2)}` },
    { title: "综合评分", dataIndex: "selectionScore", width: 110, align: "right", render: (value) => value?.toFixed(2) ?? "-" },
    { title: "结论", dataIndex: "recommendation", width: 130, render: (value) => value ? <Tag>{value}</Tag> : "-" },
    { title: "操作", width: 140, render: (_, row) => <Button danger type="text" icon={<DeleteOutlined />} aria-label={`移除 ${row.title}`} loading={remove.isPending} onClick={() => remove.mutate(row.productId)}>移除</Button> },
  ];
  return <ProtectedRoute><div className={styles.page}><AppHeader /><main className={styles.main}><div className={styles.titleRow}><div><h1>选品库</h1><p>跨市场保留个人关注商品，并可按市场筛选。</p></div><Select aria-label="筛选市场" value={market} onChange={setMarket} allowClear placeholder="全部市场" options={markets.map((value) => ({ value, label: value }))} /></div>{list.isError && <Alert type="error" showIcon message="选品库加载失败" description={list.error.message} />}<div className={styles.surface}><Table rowKey="productId" columns={columns} dataSource={list.data?.items ?? []} loading={list.isLoading} pagination={false} locale={{ emptyText: <Empty description="还没有收藏商品"><Link href="/products"><Button icon={<SettingOutlined />}>浏览商品</Button></Link></Empty> }} /></div></main></div></ProtectedRoute>;
}
