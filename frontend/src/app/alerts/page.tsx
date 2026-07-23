"use client";

import { CheckOutlined, PlusOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Drawer, Empty, Form, InputNumber, Select, Space, Switch, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import Link from "next/link";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { markets, type AlertEvent, type AlertRuleInput, userWorkflowApi } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../product-workspace.module.css";

const metricLabels: Record<AlertEvent["metricType"], string> = {
  SALES_GROWTH_7D: "7日销量增长", PRICE_DROP_7D: "7日价格下降", COMPETITION_SCORE_INCREASE: "竞争评分增加", SELECTION_SCORE_DROP: "综合评分下降",
};

export default function AlertsPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? ""; const client = useQueryClient(); const [market, setMarket] = useState<string>(); const [open, setOpen] = useState(false); const [form] = Form.useForm<AlertRuleInput>();
  const alerts = useQuery({ queryKey: ["alerts", market], queryFn: () => userWorkflowApi.alerts(accessToken, { page: 1, pageSize: 100, market }), enabled: Boolean(accessToken) });
  const rules = useQuery({ queryKey: ["alert-rules"], queryFn: () => userWorkflowApi.alertRules(accessToken), enabled: Boolean(accessToken) });
  const markRead = useMutation({ mutationFn: (id: number) => userWorkflowApi.markAlertRead(accessToken, id), onSuccess: () => client.invalidateQueries({ queryKey: ["alerts"] }) });
  const readAll = useMutation({ mutationFn: () => userWorkflowApi.markAllAlertsRead(accessToken), onSuccess: () => client.invalidateQueries({ queryKey: ["alerts"] }) });
  const createRule = useMutation({ mutationFn: (input: AlertRuleInput) => userWorkflowApi.createAlertRule(accessToken, input), onSuccess: () => { client.invalidateQueries({ queryKey: ["alert-rules"] }); form.resetFields(); setOpen(false); } });
  const updateRule = useMutation({ mutationFn: ({ id, input }: { id: number; input: AlertRuleInput }) => userWorkflowApi.updateAlertRule(accessToken, id, input), onSuccess: () => client.invalidateQueries({ queryKey: ["alert-rules"] }) });
  const columns: TableColumnsType<AlertEvent> = [
    { title: "状态", width: 82, render: (_, row) => row.readStatus ? <Tag>已读</Tag> : <Tag color="processing">未读</Tag> },
    { title: "商品", dataIndex: "productTitle", render: (title, row) => <Link href={`/products/${row.productId}`}>{title}</Link> },
    { title: "指标", dataIndex: "metricType", width: 150, render: (value: AlertEvent["metricType"]) => metricLabels[value] },
    { title: "数值 / 阈值", width: 150, align: "right", render: (_, row) => `${Number(row.metricValue).toFixed(2)} / ${Number(row.thresholdValue).toFixed(2)}` },
    { title: "统计日", dataIndex: "statDate", width: 120 },
    { title: "操作", width: 100, render: (_, row) => !row.readStatus && <Button type="text" icon={<CheckOutlined />} aria-label={`标记 ${row.productTitle} 提醒已读`} loading={markRead.isPending} onClick={() => markRead.mutate(row.id)}>已读</Button> },
  ];
  return <ProtectedRoute><div className={styles.page}><AppHeader /><main className={styles.main}>
    <div className={styles.titleRow}><div><h1>提醒</h1><p>规则只监控当前启用算法版本的同版本分析快照。</p></div><Space><Select aria-label="筛选市场" value={market} onChange={setMarket} allowClear placeholder="全部市场" options={markets.map((value) => ({ value, label: value }))} /><Button onClick={() => readAll.mutate()} loading={readAll.isPending}>全部标为已读</Button><Button type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>新建规则</Button></Space></div>
    {alerts.isError && <Alert type="error" showIcon message="提醒加载失败" description={alerts.error.message} />}
    <div className={styles.surface}><Table rowKey="id" columns={columns} dataSource={alerts.data?.items ?? []} loading={alerts.isLoading} pagination={false} locale={{ emptyText: <Empty description="暂无提醒" /> }} /></div>
    <section className={styles.ruleBand}><h2>监控规则</h2>{rules.data?.length ? <Table rowKey="id" size="small" pagination={false} dataSource={rules.data} columns={[{ title: "商品 ID", dataIndex: "productId" }, { title: "销量增长", dataIndex: "salesGrowth7dThreshold", render: (value) => value == null ? "-" : `${value}%` }, { title: "价格下降", dataIndex: "priceDrop7dThreshold", render: (value) => value == null ? "-" : `${value}%` }, { title: "竞争评分增加", dataIndex: "competitionScoreIncreaseThreshold", render: (value) => value == null ? "-" : `${value} 点` }, { title: "综合评分下降", dataIndex: "selectionScoreDropThreshold", render: (value) => value == null ? "-" : `${value} 点` }, { title: "启用", dataIndex: "enabled", render: (enabled, rule) => <Switch checked={enabled} aria-label={`切换规则 ${rule.id}`} onChange={(checked) => updateRule.mutate({ id: rule.id, input: { ...rule, enabled: checked } })} /> }]} /> : <Empty description="尚未创建监控规则" />}</section>
    <Drawer title="新建监控规则" open={open} onClose={() => setOpen(false)} destroyOnClose><Form form={form} layout="vertical" initialValues={{ enabled: true }} onFinish={(values) => createRule.mutate(values)}><Form.Item name="productId" label="商品 ID" rules={[{ required: true, message: "请输入商品 ID" }]}><InputNumber min={1} precision={0} className={styles.numberInput} /></Form.Item><Form.Item name="salesGrowth7dThreshold" label="7日销量增长阈值 (%)"><InputNumber min={0} precision={2} className={styles.numberInput} /></Form.Item><Form.Item name="priceDrop7dThreshold" label="7日价格下降阈值 (%)"><InputNumber min={0} precision={2} className={styles.numberInput} /></Form.Item><Form.Item name="competitionScoreIncreaseThreshold" label="竞争评分增加阈值 (点)"><InputNumber min={0} precision={2} className={styles.numberInput} /></Form.Item><Form.Item name="selectionScoreDropThreshold" label="综合评分下降阈值 (点)"><InputNumber min={0} precision={2} className={styles.numberInput} /></Form.Item><Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>{createRule.isError && <Alert type="error" showIcon message="创建失败" description={createRule.error.message} />}<Button htmlType="submit" type="primary" loading={createRule.isPending}>保存规则</Button></Form></Drawer>
  </main></div></ProtectedRoute>;
}
