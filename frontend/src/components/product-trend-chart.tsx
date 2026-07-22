"use client";

import * as echarts from "echarts";
import { useEffect, useRef } from "react";
import type { ProductStat } from "@/lib/products";
import styles from "@/app/product-workspace.module.css";

export function ProductTrendChart({ stats, currency }: { stats: ProductStat[]; currency: string }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!ref.current || stats.length === 0) return;
    const chart = echarts.init(ref.current);
    chart.setOption({
      animationDuration: 300,
      color: ["#006d5b", "#d92d20"],
      tooltip: { trigger: "axis" },
      legend: { data: ["当日销量", `价格 (${currency})`], top: 14, left: "center" },
      grid: { left: 54, right: 58, top: 78, bottom: 48 },
      xAxis: { type: "category", boundaryGap: false, data: stats.map((item) => item.statDate) },
      yAxis: [
        { type: "value", name: "销量", minInterval: 1 },
        { type: "value", name: currency, scale: true },
      ],
      series: [
        { name: "当日销量", type: "bar", data: stats.map((item) => item.salesVolume), barMaxWidth: 20 },
        { name: `价格 (${currency})`, type: "line", yAxisIndex: 1, smooth: true, data: stats.map((item) => item.price) },
      ],
    });
    const resize = () => chart.resize();
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      chart.dispose();
    };
  }, [currency, stats]);

  return <div ref={ref} className={styles.chart} role="img" aria-label="商品每日销量和价格趋势图" />;
}
