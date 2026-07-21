"use client";

import { SafetyCertificateFilled } from "@ant-design/icons";
import { FoundationStatus } from "@/components/foundation-status";
import styles from "./page.module.css";

export default function Home() {
  return (
    <div className={styles.appShell}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.brand}>
            <span className={styles.brandMark}>TI</span>
            <div>
              <strong>TikTok Product Insight</strong>
              <span>Operations console</span>
            </div>
          </div>
          <div className={styles.environment}>
            <SafetyCertificateFilled aria-hidden="true" />
            Foundation
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <section className={styles.pageHeading}>
          <div>
            <p>System overview</p>
            <h1>Platform foundation</h1>
          </div>
          <span className={styles.stageBadge}>Stage 01</span>
        </section>
        <FoundationStatus />
      </main>
    </div>
  );
}
