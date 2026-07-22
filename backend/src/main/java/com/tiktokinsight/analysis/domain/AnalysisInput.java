package com.tiktokinsight.analysis.domain;

import java.time.LocalDate;
import java.util.List;

public record AnalysisInput(
        long productId,
        String market,
        long categoryId,
        String currency,
        LocalDate listedDate,
        LocalDate analysisDate,
        String algorithmVersion,
        List<AnalysisDailyStat> dailyStats,
        CostProfile costProfile,
        CategoryBenchmark benchmark
) {
    public AnalysisInput {
        dailyStats = List.copyOf(dailyStats);
    }
}
