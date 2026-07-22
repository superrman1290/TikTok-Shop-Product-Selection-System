package com.tiktokinsight.analysis.dto;

import com.tiktokinsight.analysis.domain.AnalysisResult;
import com.tiktokinsight.analysis.domain.LifecycleStage;
import com.tiktokinsight.analysis.domain.Recommendation;
import com.tiktokinsight.analysis.domain.ScoreDetail;
import com.tiktokinsight.analysis.domain.ScoreStatus;
import com.tiktokinsight.product.domain.ProductDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductAnalysisResponse(
        long productId,
        String title,
        String market,
        String currency,
        LocalDate analysisDate,
        String algorithmVersion,
        ScoreStatus scoreStatus,
        BigDecimal currentPrice,
        Long salesVolume7d,
        BigDecimal salesGrowthRate7d,
        BigDecimal salesGrowthRate30d,
        BigDecimal videoGrowthRate7d,
        BigDecimal creatorGrowthRate7d,
        BigDecimal trendScore,
        BigDecimal competitionScore,
        BigDecimal estimatedProfit,
        BigDecimal estimatedProfitMargin,
        BigDecimal profitScore,
        BigDecimal riskScore,
        BigDecimal selectionScore,
        LifecycleStage lifecycleStage,
        Recommendation recommendation,
        List<String> reasons,
        List<String> risks,
        List<ScoreDetail> scoreDetails
) {
    public static ProductAnalysisResponse from(AnalysisResult result, ProductDetail product) {
        return new ProductAnalysisResponse(result.productId(), product.title(), result.market(), product.currency(), result.analysisDate(),
                result.algorithmVersion(), result.scoreStatus(), product.currentPrice(), result.salesVolume7d(), result.salesGrowthRate7d(),
                result.salesGrowthRate30d(), result.videoGrowthRate7d(), result.creatorGrowthRate7d(), result.trendScore(),
                result.competitionScore(), result.profitCalculation() == null ? null : result.profitCalculation().estimatedProfit(),
                result.profitCalculation() == null ? null : result.profitCalculation().estimatedProfitMargin(), result.profitScore(),
                result.riskScore(), result.selectionScore(), result.lifecycleStage(), result.recommendation(), result.reasons(), result.risks(),
                result.scoreDetails());
    }
}
