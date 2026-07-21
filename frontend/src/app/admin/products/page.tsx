"use client";

import { EditOutlined, SearchOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Form, Input, InputNumber, message, Modal, Select, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { ProductImage } from "@/components/product-image";
import { markets, productApi, type ProductStatus, type ProductSummary } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../../product-workspace.module.css";

interface EditValues {
  title: string;
  currentPrice: number;
  originalPrice?: number;
  imageUrl?: string;
  productUrl?: string;
  status: ProductStatus;
}

export default function AdminProductsPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const [page, setPage] = useState(1);
  const [market, setMarket] = useState<string>();
  const [keyword, setKeyword] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<ProductStatus>();
  const [editing, setEditing] = useState<ProductSummary>();
  const [form] = Form.useForm<EditValues>();
  const [messageApi, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();
  const products = useQuery({
    queryKey: ["admin-products", page, market, search, status],
    queryFn: () => productApi.adminProducts(accessToken, { page, pageSize: 20, market, keyword: search, status }),
    enabled: Boolean(accessToken),
  });
  const update = useMutation({
    mutationFn: (values: EditValues) => productApi.updateProduct(accessToken, editing?.id ?? 0, values),
    onSuccess: () => {
      messageApi.success("商品已更新");
      setEditing(undefined);
      queryClient.invalidateQueries({ queryKey: ["admin-products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (error) => messageApi.error(error.message),
  });

  const edit = (product: ProductSummary) => {
    setEditing(product);
    form.setFieldsValue({
      title: product.title,
      currentPrice: product.currentPrice,
      originalPrice: product.originalPrice,
      imageUrl: product.imageUrl,
      productUrl: product.productUrl,
      status: product.status,
    });
  };
  const columns: TableColumnsType<ProductSummary> = [
    {
      title: "商品",
      width: 420,
      render: (_, product) => (
        <div className={styles.productCell}>
          <ProductImage src={product.imageUrl} alt={product.title} className={product.imageUrl ? styles.productThumb : styles.imageFallback} />
          <span><strong>{product.title}</strong><span>{product.externalProductId}</span></span>
        </div>
      ),
    },
    { title: "市场", dataIndex: "market", width: 82 },
    { title: "分类", dataIndex: "categoryName", width: 160 },
    { title: "店铺", dataIndex: "shopName", width: 160 },
    { title: "价格", dataIndex: "currentPrice", width: 130, align: "right", render: (value, item) => `${item.currency} ${Number(value).toFixed(2)}` },
    { title: "状态", dataIndex: "status", width: 110, render: (value) => <Tag color={value === "ACTIVE" ? "success" : value === "INACTIVE" ? "warning" : "default"}>{value}</Tag> },
    { title: "编辑", key: "edit", width: 82, render: (_, product) => <Button aria-label={`编辑 ${product.title}`} icon={<EditOutlined />} onClick={() => edit(product)} /> },
  ];

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className={styles.page}>
        {contextHolder}
        <AppHeader />
        <main className={styles.main}>
          <div className={styles.titleRow}><div><h1>商品管理</h1><p>维护已导入商品的基础信息和可见状态。</p></div></div>
          <div className={styles.filters}>
            <Input value={keyword} onChange={(event) => setKeyword(event.target.value)} onPressEnter={() => { setPage(1); setSearch(keyword.trim()); }} prefix={<SearchOutlined />} placeholder="搜索商品标题" allowClear />
            <Select value={market} onChange={(value) => { setPage(1); setMarket(value); }} options={markets.map((value) => ({ value, label: value }))} placeholder="全部市场" allowClear />
            <Select value={status} onChange={(value) => { setPage(1); setStatus(value); }} options={["ACTIVE", "INACTIVE", "REMOVED"].map((value) => ({ value, label: value }))} placeholder="全部状态" allowClear />
          </div>
          {products.isError && <Alert type="error" showIcon message="商品加载失败" description={products.error.message} />}
          <div className={styles.surface}>
            <Table rowKey="id" columns={columns} dataSource={products.data?.items ?? []} loading={products.isLoading} scroll={{ x: 1120 }} locale={{ emptyText: <Empty description="暂无商品" /> }} pagination={{ current: page, pageSize: 20, total: products.data?.total ?? 0, showSizeChanger: false, onChange: setPage }} />
          </div>
          <Modal open={Boolean(editing)} title="编辑商品" okText="保存" cancelText="取消" confirmLoading={update.isPending} onCancel={() => setEditing(undefined)} onOk={() => form.validateFields().then((values) => update.mutate(values))}>
            <Form form={form} layout="vertical">
              <Form.Item name="title" label="商品标题" rules={[{ required: true }, { max: 500 }]}><Input /></Form.Item>
              <Form.Item name="currentPrice" label="当前价格" rules={[{ required: true }]}><InputNumber min={0} precision={2} style={{ width: "100%" }} /></Form.Item>
              <Form.Item name="originalPrice" label="原价"><InputNumber min={0} precision={2} style={{ width: "100%" }} /></Form.Item>
              <Form.Item name="imageUrl" label="图片 URL" rules={[{ type: "url" }, { max: 1500 }]}><Input /></Form.Item>
              <Form.Item name="productUrl" label="商品 URL" rules={[{ type: "url" }, { max: 1500 }]}><Input /></Form.Item>
              <Form.Item name="status" label="状态" rules={[{ required: true }]}><Select options={["ACTIVE", "INACTIVE", "REMOVED"].map((value) => ({ value, label: value }))} /></Form.Item>
            </Form>
          </Modal>
        </main>
      </div>
    </ProtectedRoute>
  );
}
