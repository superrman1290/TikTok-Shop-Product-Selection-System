"use client";

import {
  ApiOutlined,
  CheckCircleFilled,
  CloudServerOutlined,
  CloseCircleFilled,
  DatabaseOutlined,
  FolderOpenOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { Alert, Button, Card, Spin, Tag, Tooltip } from "antd";
import { useQuery } from "@tanstack/react-query";
import type { ReactNode } from "react";
import { fetchSystemHealth, type ComponentStatus, type SystemHealth } from "@/lib/health";
import styles from "./foundation-status.module.css";

interface ComponentDefinition {
  key: keyof SystemHealth;
  label: string;
  detail: string;
  icon: ReactNode;
}

const components: ComponentDefinition[] = [
  { key: "application", label: "Application", detail: "Spring Boot API", icon: <ApiOutlined /> },
  { key: "database", label: "Database", detail: "MySQL 8.4", icon: <DatabaseOutlined /> },
  { key: "redis", label: "Cache", detail: "Redis 7", icon: <CloudServerOutlined /> },
  { key: "storage", label: "Object storage", detail: "Local volume", icon: <FolderOpenOutlined /> },
];

function StatusTag({ status }: { status: ComponentStatus }) {
  const isUp = status === "UP";
  return (
    <Tag
      color={isUp ? "success" : "error"}
      icon={isUp ? <CheckCircleFilled /> : <CloseCircleFilled />}
      className={styles.statusTag}
    >
      {status}
    </Tag>
  );
}

export function FoundationStatus() {
  const query = useQuery({
    queryKey: ["system-health"],
    queryFn: ({ signal }) => fetchSystemHealth(signal),
    refetchInterval: 30_000,
  });

  const checkedAt = query.data?.timestamp
    ? new Intl.DateTimeFormat("zh-CN", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      }).format(new Date(query.data.timestamp))
    : "--:--:--";

  return (
    <section aria-labelledby="service-health-heading" className={styles.section}>
      <div className={styles.sectionHeader}>
        <div>
          <p className={styles.eyebrow}>Runtime</p>
          <h2 id="service-health-heading">Service health</h2>
          <p className={styles.timestamp}>Last check {checkedAt}</p>
        </div>
        <Tooltip title="Refresh service health">
          <Button
            aria-label="Refresh service health"
            icon={<ReloadOutlined />}
            loading={query.isFetching}
            onClick={() => void query.refetch()}
          />
        </Tooltip>
      </div>

      {query.isLoading ? (
        <div className={styles.loading} role="status">
          <Spin size="large" />
          <span>Checking services</span>
        </div>
      ) : null}

      {query.isError ? (
        <Alert
          type="error"
          showIcon
          message="Health endpoint unavailable"
          description="The application could not read the current service state."
          action={<Button onClick={() => void query.refetch()}>Retry</Button>}
        />
      ) : null}

      {query.data ? (
        <div className={styles.statusGrid}>
          {components.map((component) => (
            <Card key={component.key} className={styles.statusCard}>
              <div className={styles.cardTopline}>
                <span className={styles.icon} aria-hidden="true">{component.icon}</span>
                <StatusTag status={query.data.data[component.key]} />
              </div>
              <h3>{component.label}</h3>
              <p>{component.detail}</p>
            </Card>
          ))}
        </div>
      ) : null}
    </section>
  );
}
