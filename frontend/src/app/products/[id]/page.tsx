"use client";

import { ArrowLeftOutlined, ExportOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Empty, Spin, Tag } from "antd";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AppHeader } from "@/components/app-header";
import { ProductTrendChart } from "@/components/product-trend-chart";
import { ProductImage } from "@/components/product-image";
import { ProtectedRoute } from "@/components/protected-route";
import { productApi } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../../product-workspace.module.css";

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const productId = Number(params.id);
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const product = useQuery({
    queryKey: ["product", productId],
    queryFn: () => productApi.get(accessToken, productId),
    enabled: Boolean(accessToken && Number.isFinite(productId)),
  });
  const stats = useQuery({
    queryKey: ["product-stats", productId],
    queryFn: () => productApi.stats(accessToken, productId),
    enabled: Boolean(accessToken && Number.isFinite(productId)),
  });

  return (
    <ProtectedRoute>
      <div className={styles.page}>
        <AppHeader />
        <main className={styles.main}>
          <div className={styles.titleRow}>
            <Link href="/products"><Button icon={<ArrowLeftOutlined />}>返回商品列表</Button></Link>
          </div>
          {product.isLoading && <Spin size="large" />}
          {product.isError && <Alert type="error" showIcon message="商品加载失败" description={product.error.message} />}
          {product.data && (
            <>
              <section className={styles.detailGrid}>
                <ProductImage
                  src={product.data.imageUrl}
                  alt={product.data.title}
                  className={`${styles.media} ${product.data.imageUrl ? "" : styles.mediaFallback}`}
                />
                <div className={styles.detailBody}>
                  <Tag>{product.data.market}</Tag>
                  <h1>{product.data.title}</h1>
                  <div className={styles.price}>{product.data.currency} {Number(product.data.currentPrice).toFixed(2)}</div>
                  <div className={styles.facts}>
                    <div className={styles.fact}><span>分类</span><strong>{product.data.categoryName}</strong></div>
                    <div className={styles.fact}><span>店铺</span><strong>{product.data.shopName}</strong></div>
                    <div className={styles.fact}><span>评分 / 评价</span><strong>{product.data.rating ?? "-"} / {product.data.reviewCount}</strong></div>
                  </div>
                  {product.data.productUrl && (
                    <p><a href={product.data.productUrl} target="_blank" rel="noreferrer"><ExportOutlined /> 打开商品来源页面</a></p>
                  )}
                </div>
              </section>
              <section className={styles.chartBand}>
                <h2>每日销量与价格趋势</h2>
                {stats.isLoading && <Spin />}
                {stats.isError && <Alert type="error" showIcon message="趋势加载失败" description={stats.error.message} />}
                {stats.data && stats.data.length > 0 && <ProductTrendChart stats={stats.data} currency={product.data.currency} />}
                {stats.data?.length === 0 && <Empty description="尚未导入每日统计数据" />}
              </section>
            </>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
