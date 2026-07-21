import { SafetyCertificateOutlined, ShopOutlined } from "@ant-design/icons";
import type { PropsWithChildren, ReactNode } from "react";
import styles from "./auth-shell.module.css";

interface AuthShellProps extends PropsWithChildren {
  title: string;
  subtitle: string;
  footer: ReactNode;
}

export function AuthShell({ title, subtitle, footer, children }: AuthShellProps) {
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <div className={styles.brandMark}>TI</div>
        <div className={styles.brandText}>
          <strong>TikTok Product Insight</strong>
          <span>跨境选品决策系统</span>
        </div>
      </header>

      <div className={styles.content}>
        <section className={styles.context} aria-label="平台信息">
          <div className={styles.contextIcon}><ShopOutlined /></div>
          <p className={styles.eyebrow}>TikTok Shop operations</p>
          <h1>从市场信号到选品判断</h1>
          <div className={styles.environment}>
            <SafetyCertificateOutlined aria-hidden="true" />
            <span>安全认证环境</span>
            <strong>Online</strong>
          </div>
        </section>

        <section className={styles.formPanel} aria-labelledby="auth-title">
          <div className={styles.formHeading}>
            <h2 id="auth-title">{title}</h2>
            <p>{subtitle}</p>
          </div>
          {children}
          <div className={styles.footer}>{footer}</div>
        </section>
      </div>
    </main>
  );
}
