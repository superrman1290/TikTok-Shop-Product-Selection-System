package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalysisResult(
        long productId,
        String market,
        long categoryId,
        LocalDate analysisDate,
        String algorithmVersion,
        ScoreStatus scoreStatus,
        Long salesVolume7d,
        BigDecimal salesGrowthRate7d,
        BigDecimal salesGrowthRate30d,
        BigDecimal videoGrowthRate7d,
        BigDecimal creatorGrowthRate7d,
        BigDecimal trendScore,
        BigDecimal competitionScore,
        ProfitCalculation profitCalculation,
        BigDecimal profitScore,
        BigDecimal riskScore,
        BigDecimal selectionScore,
        LifecycleStage lifecycleStage,
        Recommendation recommendation,
        List<ScoreDetail> scoreDetails,
        List<String> reasons,
        List<String> risks
) {
    public AnalysisResult {
        scoreDetails = List.copyOf(scoreDetails);
        reasons = List.copyOf(reasons);
        risks = List.copyOf(risks);
    }
}
