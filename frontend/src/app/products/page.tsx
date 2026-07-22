"use client";

import { SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Empty, Input, InputNumber, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import Link from "next/link";
import { useEffect, useState } from "react";
import { AppHeader } from "@/components/app-header";
import { ProductImage } from "@/components/product-image";
import { ProtectedRoute } from "@/components/protected-route";
import { markets, productApi, type ProductSummary } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import { useMarketStore } from "@/store/market-store";
import styles from "../product-workspace.module.css";

const sortOptions = [
  { value: "collectedAt", label: "最近采集" },
  { value: "rating", label: "评分" },
  { value: "reviewCount", label: "评价数" },
  { value: "currentPrice", label: "当前价格" },
  { value: "listedAt", label: "上架时间" },
];

export default function ProductsPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const [page, setPage] = useState(1);
  const selectedMarket = useMarketStore((state) => state.selectedMarket);
  const [market, setMarket] = useState<string>(selectedMarket);
  const [keyword, setKeyword] = useState("");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState("collectedAt");
  const [direction, setDirection] = useState("desc");
  const [lifecycleStage, setLifecycleStage] = useState<string>();
  const [recommendation, setRecommendation] = useState<string>();
  const [minPrice, setMinPrice] = useState<number | null>(null);
  const [maxPrice, setMaxPrice] = useState<number | null>(null);
  const [minSelectionScore, setMinSelectionScore] = useState<number | null>(null);
  useEffect(() => { setMarket(selectedMarket); setPage(1); }, [selectedMarket]);
  const products = useQuery({
    queryKey: ["products", page, market, search, sortBy, direction, lifecycleStage, recommendation, minPrice, maxPrice, minSelectionScore],
    queryFn: () => productApi.list(accessToken, { page, pageSize: 20, market, keyword: search, sortBy, direction, lifecycleStage, recommendation, minPrice: minPrice ?? undefined, maxPrice: maxPrice ?? undefined, minSelectionScore: minSelectionScore ?? undefined }),
    enabled: Boolean(accessToken),
  });

  const columns: TableColumnsType<ProductSummary> = [
    {
      title: "商品",
      dataIndex: "title",
      width: 420,
      render: (_, product) => (
        <Link className={styles.productCell} href={`/products/${product.id}`}>
          <ProductImage src={product.imageUrl} alt={product.title} className={product.imageUrl ? styles.productThumb : styles.imageFallback} />
          <span>
            <strong>{product.title}</strong>
            <span>{product.externalProductId}</span>
          </span>
        </Link>
      ),
    },
    { title: "市场", dataIndex: "market", width: 82, render: (value) => <Tag>{value}</Tag> },
    { title: "分类", dataIndex: "categoryName", width: 160 },
    { title: "店铺", dataIndex: "shopName", width: 170 },
    {
      title: "价格",
      dataIndex: "currentPrice",
      width: 130,
      align: "right",
      render: (value, product) => `${product.currency} ${Number(value).toFixed(2)}`,
    },
    { title: "评分", dataIndex: "rating", width: 90, align: "right", render: (value) => value ?? "-" },
    { title: "评价数", dataIndex: "reviewCount", width: 110, align: "right" },
  ];

  return (
    <ProtectedRoute>
      <div className={styles.page}>
        <AppHeader />
        <main className={styles.main}>
          <div className={styles.titleRow}>
            <div><h1>商品榜单</h1><p>按市场浏览已导入的 TikTok Shop 商品基础数据。</p></div>
          </div>
          <div className={styles.filters}>
            <Input
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onPressEnter={() => { setPage(1); setSearch(keyword.trim()); }}
              prefix={<SearchOutlined />}
              placeholder="搜索商品标题"
              allowClear
            />
            <Select
              value={market}
              onChange={(value) => { setPage(1); setMarket(value); }}
              options={markets.map((value) => ({ value, label: value }))}
              placeholder="全部市场"
              allowClear
            />
            <Select value={lifecycleStage} onChange={(value) => { setPage(1); setLifecycleStage(value); }} allowClear placeholder="生命周期" options={["NEW", "GROWTH", "EXPLOSIVE", "MATURE", "DECLINE"].map((value) => ({ value, label: value }))} />
            <Select value={recommendation} onChange={(value) => { setPage(1); setRecommendation(value); }} allowClear placeholder="推荐结论" options={["RECOMMENDED", "WATCH", "NOT_RECOMMENDED", "DATA_INSUFFICIENT"].map((value) => ({ value, label: value }))} />
            <InputNumber aria-label="最低价格" min={0} value={minPrice} onChange={(value) => { setPage(1); setMinPrice(value); }} placeholder="最低价格" />
            <InputNumber aria-label="最高价格" min={0} value={maxPrice} onChange={(value) => { setPage(1); setMaxPrice(value); }} placeholder="最高价格" />
            <InputNumber aria-label="最低综合评分" min={0} max={100} value={minSelectionScore} onChange={(value) => { setPage(1); setMinSelectionScore(value); }} placeholder="最低综合评分" />
            <Select value={sortBy} onChange={(value) => { setPage(1); setSortBy(value); }} options={sortOptions} />
            <Select
              value={direction}
              onChange={(value) => { setPage(1); setDirection(value); }}
              options={[{ value: "desc", label: "降序" }, { value: "asc", label: "升序" }]}
            />
          </div>
          {products.isError && <Alert type="error" showIcon message="商品加载失败" description={products.error.message} />}
          <div className={styles.surface}>
            <Table
              rowKey="id"
              columns={columns}
              dataSource={products.data?.items ?? []}
              loading={products.isLoading}
              scroll={{ x: 1160 }}
              locale={{ emptyText: <Empty description="当前筛选条件下没有商品" /> }}
              pagination={{
                current: page,
                pageSize: 20,
                total: products.data?.total ?? 0,
                showSizeChanger: false,
                onChange: setPage,
              }}
            />
          </div>
        </main>
      </div>
    </ProtectedRoute>
  );
}
