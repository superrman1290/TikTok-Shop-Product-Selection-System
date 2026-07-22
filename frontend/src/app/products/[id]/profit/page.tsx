"use client";

import { ArrowLeftOutlined, DeleteOutlined, SaveOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Descriptions, Empty, Form, InputNumber, Spin, Tag } from "antd";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { productApi, type CostProfile } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../../../product-workspace.module.css";

const numericKeys: Array<keyof Omit<CostProfile, "currency">> = [
  "purchaseCost", "domesticShippingCost", "internationalShippingCost", "platformCommissionRate",
  "paymentFeeRate", "advertisingCostRate", "refundLossRate", "otherCost",
];

const labels: Record<keyof Omit<CostProfile, "currency">, string> = {
  purchaseCost: "采购成本",
  domesticShippingCost: "国内物流成本",
  internationalShippingCost: "国际物流成本",
  platformCommissionRate: "平台佣金率 (%)",
  paymentFeeRate: "支付费率 (%)",
  advertisingCostRate: "广告成本率 (%)",
  refundLossRate: "退款损耗率 (%)",
  otherCost: "其他成本",
};

export default function ProductProfitPage() {
  const params = useParams<{ id: string }>();
  const productId = Number(params.id);
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const queryClient = useQueryClient();
  const [profile, setProfile] = useState<CostProfile>();
  const [calculation, setCalculation] = useState<Awaited<ReturnType<typeof productApi.profitCalculation>>>();
  const product = useQuery({ queryKey: ["product", productId], queryFn: () => productApi.get(accessToken, productId), enabled: Boolean(accessToken && Number.isFinite(productId)) });
  const cost = useQuery({ queryKey: ["cost-profile", productId], queryFn: () => productApi.costProfile(accessToken, productId), enabled: Boolean(accessToken && Number.isFinite(productId)) });

  useEffect(() => {
    if (cost.data) setProfile(cost.data.profile);
  }, [cost.data]);

  const calculate = useMutation({
    mutationFn: (input: CostProfile) => productApi.profitCalculation(accessToken, productId, Number(product.data?.currentPrice), input),
    onSuccess: setCalculation,
  });
  const save = useMutation({
    mutationFn: (input: CostProfile) => productApi.saveCostProfile(accessToken, productId, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cost-profile", productId] }),
  });
  const remove = useMutation({
    mutationFn: () => productApi.deleteCostProfile(accessToken, productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cost-profile", productId] }),
  });

  const update = (key: keyof Omit<CostProfile, "currency">, value: number | null) => {
    setProfile((current) => current ? { ...current, [key]: value ?? 0 } : current);
  };

  return (
    <ProtectedRoute>
      <div className={styles.page}>
        <AppHeader />
        <main className={styles.main}>
          <div className={styles.titleRow}>
            <div>
              <Link href={`/products/${productId}`}><Button icon={<ArrowLeftOutlined />}>返回商品</Button></Link>
              <h1 className={styles.profitTitle}>利润测算</h1>
              {product.data && <p>{product.data.title} · {product.data.currency} {Number(product.data.currentPrice).toFixed(2)}</p>}
            </div>
            {cost.data && <Tag>{cost.data.source === "USER" ? "个人成本" : "市场默认成本"}</Tag>}
          </div>
          {(product.isLoading || cost.isLoading) && <Spin size="large" />}
          {(product.isError || cost.isError) && <Alert type="error" showIcon message="成本数据加载失败" description={(product.error ?? cost.error)?.message} />}
          {profile && product.data && (
            <div className={styles.profitGrid}>
              <section className={styles.profitForm}>
                <Form layout="vertical">
                  {numericKeys.map((key) => (
                    <Form.Item key={key} label={labels[key]}>
                      <InputNumber
                        min={0}
                        precision={key.endsWith("Rate") ? 4 : 2}
                        value={profile[key]}
                        onChange={(value) => update(key, value)}
                        controls
                        className={styles.numberInput}
                        aria-label={labels[key]}
                      />
                    </Form.Item>
                  ))}
                  <div className={styles.profitActions}>
                    <Button type="primary" loading={calculate.isPending} onClick={() => calculate.mutate(profile)}>重新测算</Button>
                    <Button icon={<SaveOutlined />} loading={save.isPending} onClick={() => save.mutate(profile)}>保存个人成本</Button>
                    {cost.data?.source === "USER" && <Button danger icon={<DeleteOutlined />} loading={remove.isPending} onClick={() => remove.mutate()}>恢复市场默认</Button>}
                  </div>
                </Form>
                {(calculate.isError || save.isError || remove.isError) && <Alert className={styles.actionError} type="error" showIcon message="操作失败" description={(calculate.error ?? save.error ?? remove.error)?.message} />}
              </section>
              <section className={styles.profitResult}>
                {calculation ? (
                  <Descriptions bordered column={1} size="small">
                    <Descriptions.Item label="销售价">{product.data.currency} {calculation.sellingPrice.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="平台佣金">{product.data.currency} {calculation.platformCommission.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="支付费用">{product.data.currency} {calculation.paymentFee.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="广告成本">{product.data.currency} {calculation.advertisingCost.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="退款损耗">{product.data.currency} {calculation.refundLoss.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="总成本">{product.data.currency} {calculation.totalCost.toFixed(2)}</Descriptions.Item>
                    <Descriptions.Item label="预计利润"><strong>{product.data.currency} {calculation.estimatedProfit.toFixed(2)}</strong></Descriptions.Item>
                    <Descriptions.Item label="利润率"><strong>{calculation.estimatedProfitMargin.toFixed(2)}%</strong></Descriptions.Item>
                    <Descriptions.Item label="成本利润率">{calculation.costProfitMargin?.toFixed(2) ?? "-"}%</Descriptions.Item>
                  </Descriptions>
                ) : <Empty description="填写成本后执行测算" />}
              </section>
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
