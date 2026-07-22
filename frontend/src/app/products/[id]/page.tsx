"use client";

import { ArrowLeftOutlined, BookOutlined, CalculatorOutlined, ExportOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Descriptions, Empty, Spin, Tag } from "antd";
import Link from "next/link";
import { useParams } from "next/navigation";
import { AppHeader } from "@/components/app-header";
import { ProductTrendChart } from "@/components/product-trend-chart";
import { AnalysisScoreChart } from "@/components/analysis-score-chart";
import { ProductImage } from "@/components/product-image";
import { ProtectedRoute } from "@/components/protected-route";
import { productApi, userWorkflowApi } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../../product-workspace.module.css";

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const productId = Number(params.id);
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const queryClient = useQueryClient();
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
  const analysis = useQuery({
    queryKey: ["product-analysis", productId],
    queryFn: () => productApi.analysis(accessToken, productId),
    enabled: Boolean(accessToken && Number.isFinite(productId)),
  });
  const addToWatchlist = useMutation({
    mutationFn: () => userWorkflowApi.addWatchlist(accessToken, productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["watchlist"] }),
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
                  <div className={styles.profitActions}>
                    <Link href={`/products/${productId}/profit`}><Button icon={<CalculatorOutlined />}>利润测算</Button></Link>
                    <Button icon={<BookOutlined />} loading={addToWatchlist.isPending} onClick={() => addToWatchlist.mutate()}>加入选品库</Button>
                  </div>
                  {addToWatchlist.isError && <Alert className={styles.actionError} type="warning" showIcon message="加入选品库失败" description={addToWatchlist.error.message} />}
                </div>
              </section>
              <section className={styles.analysisBand}>
                <div className={styles.analysisHeading}>
                  <div><h2>选品分析</h2><p>算法版本 selection-v1.0，数据日期以商品最近统计为准。</p></div>
                  {analysis.data && <Tag>{analysis.data.recommendation}</Tag>}
                </div>
                {analysis.isLoading && <Spin />}
                {analysis.isError && <Alert type="warning" showIcon message="分析结果暂不可用" description={analysis.error.message} />}
                {analysis.data && (
                  <div className={styles.analysisGrid}>
                    <div className={styles.analysisScores}>
                      <div><span>综合评分</span><strong>{analysis.data.selectionScore?.toFixed(2) ?? "数据不足"}</strong></div>
                      <div><span>生命周期</span><strong>{analysis.data.lifecycleStage ?? "-"}</strong></div>
                      <div><span>近7日销量</span><strong>{analysis.data.salesVolume7d?.toLocaleString() ?? "-"}</strong></div>
                      <div><span>利润率</span><strong>{analysis.data.estimatedProfitMargin?.toFixed(2) ?? "-"}%</strong></div>
                    </div>
                    <AnalysisScoreChart analysis={analysis.data} />
                    <Descriptions className={styles.scoreDetails} size="small" column={2} bordered>
                      <Descriptions.Item label="趋势评分">{analysis.data.trendScore?.toFixed(2) ?? "-"}</Descriptions.Item>
                      <Descriptions.Item label="竞争评分">{analysis.data.competitionScore?.toFixed(2) ?? "-"}</Descriptions.Item>
                      <Descriptions.Item label="利润评分">{analysis.data.profitScore?.toFixed(2) ?? "-"}</Descriptions.Item>
                      <Descriptions.Item label="风险评分">{analysis.data.riskScore?.toFixed(2) ?? "-"}</Descriptions.Item>
                    </Descriptions>
                    <div className={styles.reasonGrid}>
                      <div><h3>推荐依据</h3>{analysis.data.reasons.length ? <ul>{analysis.data.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul> : <span>当前没有满足阈值的正向指标。</span>}</div>
                      <div><h3>风险提示</h3>{analysis.data.risks.length ? <ul>{analysis.data.risks.map((risk) => <li key={risk}>{risk}</li>)}</ul> : <span>当前没有满足阈值的风险指标。</span>}</div>
                    </div>
                  </div>
                )}
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
