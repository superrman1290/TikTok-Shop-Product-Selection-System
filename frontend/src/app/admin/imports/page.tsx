"use client";

import { FileSearchOutlined, InboxOutlined, UploadOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, message, Modal, Table, Tag, Upload } from "antd";
import type { TableColumnsType, UploadFile } from "antd";
import { useState } from "react";
import { AppHeader } from "@/components/app-header";
import { ProtectedRoute } from "@/components/protected-route";
import { productApi, type ImportJob, type ImportRowError, type ImportStatus } from "@/lib/products";
import { useAuthStore } from "@/store/auth-store";
import styles from "../../product-workspace.module.css";

const statusColors: Record<ImportStatus, string> = {
  PENDING: "default",
  RUNNING: "processing",
  SUCCESS: "success",
  PARTIAL_SUCCESS: "warning",
  FAILED: "error",
  CANCELLED: "default",
};

export default function AdminImportsPage() {
  const accessToken = useAuthStore((state) => state.accessToken) ?? "";
  const [productFiles, setProductFiles] = useState<UploadFile[]>([]);
  const [statFiles, setStatFiles] = useState<UploadFile[]>([]);
  const [selectedJob, setSelectedJob] = useState<ImportJob>();
  const [messageApi, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();
  const imports = useQuery({
    queryKey: ["imports"],
    queryFn: () => productApi.imports(accessToken),
    enabled: Boolean(accessToken),
  });
  const errors = useQuery({
    queryKey: ["import-errors", selectedJob?.id],
    queryFn: () => productApi.importErrors(accessToken, selectedJob?.id ?? 0),
    enabled: Boolean(accessToken && selectedJob),
  });
  const upload = useMutation({
    mutationFn: ({ type, file }: { type: "products" | "product-stats"; file: File }) =>
      productApi.upload(accessToken, type, file),
    onSuccess: (job) => {
      messageApi.success(`导入任务 #${job.id} 已完成`);
      setProductFiles([]);
      setStatFiles([]);
      queryClient.invalidateQueries({ queryKey: ["imports"] });
    },
    onError: (error) => messageApi.error(error.message),
  });

  const jobColumns: TableColumnsType<ImportJob> = [
    { title: "任务", dataIndex: "id", width: 86, render: (value) => `#${value}` },
    { title: "类型", dataIndex: "importType", width: 130, render: (value) => value === "PRODUCT" ? "商品" : "每日统计" },
    { title: "文件", dataIndex: "originalFileName", ellipsis: true },
    { title: "状态", dataIndex: "status", width: 150, render: (value: ImportStatus) => <Tag color={statusColors[value]}>{value}</Tag> },
    { title: "总行数", dataIndex: "totalRows", width: 100, align: "right" },
    { title: "成功", dataIndex: "successRows", width: 90, align: "right" },
    { title: "失败", dataIndex: "failedRows", width: 90, align: "right" },
    { title: "创建人", dataIndex: "createdByUsername", width: 120 },
    {
      title: "错误",
      key: "errors",
      width: 90,
      render: (_, job) => <Button aria-label={`查看任务 ${job.id} 错误`} icon={<FileSearchOutlined />} disabled={job.failedRows === 0} onClick={() => setSelectedJob(job)} />,
    },
  ];
  const errorColumns: TableColumnsType<ImportRowError> = [
    { title: "行", dataIndex: "rowNumber", width: 70 },
    { title: "字段", dataIndex: "fieldName", width: 150 },
    { title: "原值", dataIndex: "rawValue", width: 220, render: (value) => <div className={styles.errorValue} title={value}>{value || "-"}</div> },
    { title: "错误码", dataIndex: "errorCode", width: 170 },
    { title: "原因", dataIndex: "errorMessage" },
  ];

  const picker = (
    type: "products" | "product-stats",
    files: UploadFile[],
    setFiles: (files: UploadFile[]) => void,
  ) => (
    <div className={styles.uploadActions}>
      <Upload
        accept=".csv,text/csv"
        fileList={files}
        maxCount={1}
        beforeUpload={() => false}
        onChange={({ fileList }) => setFiles(fileList.slice(-1))}
      >
        <Button icon={<InboxOutlined />}>选择 CSV</Button>
      </Upload>
      <Button
        type="primary"
        icon={<UploadOutlined />}
        loading={upload.isPending}
        disabled={!files[0]?.originFileObj}
        onClick={() => {
          const file = files[0]?.originFileObj;
          if (file) upload.mutate({ type, file });
        }}
      >
        开始导入
      </Button>
    </div>
  );

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className={styles.page}>
        {contextHolder}
        <AppHeader />
        <main className={styles.main}>
          <div className={styles.titleRow}><div><h1>CSV 导入</h1><p>商品基础数据与每日统计数据。</p></div></div>
          <div className={styles.uploadGrid}>
            <section className={styles.uploadPanel}>
              <h2>商品数据</h2><p>创建或幂等更新分类、店铺和商品。</p>
              {picker("products", productFiles, setProductFiles)}
            </section>
            <section className={styles.uploadPanel}>
              <h2>每日统计</h2><p>幂等更新历史统计及商品最新价格。</p>
              {picker("product-stats", statFiles, setStatFiles)}
            </section>
          </div>
          {imports.isError && <Alert type="error" showIcon message="导入任务加载失败" description={imports.error.message} />}
          <h2 className={styles.sectionTitle}>最近导入任务</h2>
          <div className={styles.surface}>
            <Table
              rowKey="id"
              columns={jobColumns}
              dataSource={imports.data?.items ?? []}
              loading={imports.isLoading}
              scroll={{ x: 1050 }}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无导入任务" /> }}
            />
          </div>
          <Modal open={Boolean(selectedJob)} title={`任务 #${selectedJob?.id} 导入错误`} width={980} footer={null} onCancel={() => setSelectedJob(undefined)}>
            <Table
              rowKey={(row) => `${row.rowNumber}-${row.fieldName}-${row.errorCode}`}
              columns={errorColumns}
              dataSource={errors.data?.items ?? []}
              loading={errors.isLoading}
              scroll={{ x: 850 }}
              pagination={false}
              locale={{ emptyText: <Empty description="没有导入错误" /> }}
            />
          </Modal>
        </main>
      </div>
    </ProtectedRoute>
  );
}
