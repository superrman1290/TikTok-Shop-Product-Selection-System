"use client";

import * as echarts from "echarts";
import { useEffect, useRef } from "react";
import type { ProductAnalysis } from "@/lib/products";

export function AnalysisScoreChart({ analysis }: { analysis: ProductAnalysis }) {
  const target = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!target.current) return;
    const chart = echarts.init(target.current);
    const entries = [
      ["趋势", analysis.trendScore],
      ["低竞争", analysis.competitionScore === undefined ? undefined : 100 - analysis.competitionScore],
      ["利润", analysis.profitScore],
      ["低风险", analysis.riskScore === undefined ? undefined : 100 - analysis.riskScore],
    ].filter((entry): entry is [string, number] => entry[1] !== undefined);
    chart.setOption({
      animation: false,
      tooltip: { trigger: "axis" },
      radar: { indicator: entries.map(([name]) => ({ name, max: 100 })), radius: "62%" },
      series: [{
        type: "radar",
        data: [{ value: entries.map(([, value]) => value), name: "选品评分" }],
        areaStyle: { color: "rgba(22, 119, 255, 0.18)" },
        lineStyle: { color: "#1677ff" },
        itemStyle: { color: "#1677ff" },
      }],
    });
    const observer = new ResizeObserver(() => chart.resize());
    observer.observe(target.current);
    return () => { observer.disconnect(); chart.dispose(); };
  }, [analysis]);

  return <div className="analysisScoreChart" ref={target} aria-label="商品分析评分图表" />;
}
