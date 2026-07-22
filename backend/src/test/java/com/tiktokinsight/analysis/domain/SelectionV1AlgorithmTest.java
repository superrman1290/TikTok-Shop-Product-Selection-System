package com.tiktokinsight.analysis.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SelectionV1AlgorithmTest {

    private static final LocalDate ANALYSIS_DATE = LocalDate.of(2026, 7, 30);
    private static final CostProfile COSTS = new CostProfile("USD", bd("6.50"), bd("0.80"), bd("5.20"), bd("6"), bd("2.9"), bd("12"), bd("3"), bd("1"));
    private final SelectionV1Algorithm algorithm = new SelectionV1Algorithm();

    @Test
    void returnsDataInsufficientForFewerThanFourteenContinuousDays() {
        AnalysisResult result = analyze(stats(13, 10, 20), COSTS, new CategoryBenchmark(0, null), 30);

        assertThat(result.scoreStatus()).isEqualTo(ScoreStatus.DATA_INSUFFICIENT);
        assertThat(result.selectionScore()).isNull();
        assertThat(result.recommendation()).isEqualTo(Recommendation.DATA_INSUFFICIENT);
    }

    @Test
    void calculatesPartialScoresWithExactlyFourteenDays() {
        AnalysisResult result = analyze(stats(14, 10, 20), COSTS, new CategoryBenchmark(0, null), 30);

        assertThat(result.scoreStatus()).isEqualTo(ScoreStatus.PARTIAL);
        assertThat(result.salesGrowthRate7d()).isEqualByComparingTo("100.00");
        assertThat(result.salesGrowthRate30d()).isNull();
        assertThat(result.trendScore()).isNotNull();
    }

    @Test
    void calculatesCompleteScoresWithThirtyDaysAndThirtyDayPriceDrop() {
        AnalysisResult result = analyze(stats(30, 10, 20), COSTS, new CategoryBenchmark(0, null), 30);

        assertThat(result.scoreStatus()).isEqualTo(ScoreStatus.COMPLETE);
        assertThat(result.salesGrowthRate30d()).isNotNull();
        assertThat(result.scoreDetails()).extracting(ScoreDetail::metricCode).contains("PRICE_PRESSURE");
    }

    @Test
    void usesOneAsPreviousSalesDenominatorWhenPreviousPeriodIsZero() {
        AnalysisResult result = analyze(stats(14, 0, 10), COSTS, new CategoryBenchmark(0, null), 30);

        assertThat(result.salesGrowthRate7d()).isEqualByComparingTo("7000.00");
    }

    @Test
    void usesMarketBenchmarkForExplosiveLifecycle() {
        AnalysisResult result = analyze(stats(30, 10, 30), COSTS, new CategoryBenchmark(50, 150L), 30);

        assertThat(result.lifecycleStage()).isEqualTo(LifecycleStage.EXPLOSIVE);
    }

    @Test
    void keepsNewLifecycleAheadOfGrowthAndExplosiveRules() {
        AnalysisResult result = analyze(stats(14, 10, 100), COSTS, new CategoryBenchmark(50, 100L), 14);

        assertThat(result.lifecycleStage()).isEqualTo(LifecycleStage.NEW);
    }

    @Test
    void returnsZeroProfitScoreForNegativeProfit() {
        CostProfile expensive = new CostProfile("USD", bd("80"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"));
        AnalysisResult result = analyze(stats(30, 10, 20), expensive, new CategoryBenchmark(0, null), 30);

        assertThat(result.profitCalculation().estimatedProfit()).isNegative();
        assertThat(result.profitScore()).isEqualByComparingTo("0.00");
    }

    @Test
    void renormalizesCompetitionAndRiskWhenOptionalMetricsAreMissing() {
        List<AnalysisDailyStat> values = stats(30, 10, 20);
        AnalysisDailyStat latest = values.getLast();
        values.set(values.size() - 1, new AnalysisDailyStat(latest.statDate(), latest.price(), latest.salesVolume(), latest.videoCount(),
                latest.creatorCount(), latest.shopCount(), latest.similarProductCount(), null, null, null, latest.rating()));

        AnalysisResult result = analyze(values, COSTS, new CategoryBenchmark(0, null), 30);
        assertThat(result.competitionScore()).isNotNull();
        assertThat(result.riskScore()).isNotNull();
    }

    @Test
    void appliesRecommendationThresholdsAtExactBoundaries() {
        assertThat(algorithm.recommendationForScore(bd("59.99"))).isEqualTo(Recommendation.NOT_RECOMMENDED);
        assertThat(algorithm.recommendationForScore(bd("60.00"))).isEqualTo(Recommendation.WATCH);
        assertThat(algorithm.recommendationForScore(bd("74.99"))).isEqualTo(Recommendation.WATCH);
        assertThat(algorithm.recommendationForScore(bd("75.00"))).isEqualTo(Recommendation.RECOMMENDED);
        assertThat(algorithm.recommendationForScore(null)).isEqualTo(Recommendation.DATA_INSUFFICIENT);
    }

    @Test
    void recognizesGrowthDeclineAndMatureLifecycleBranches() {
        assertThat(analyze(stats(30, 10, 15), COSTS, new CategoryBenchmark(0, null), 30).lifecycleStage()).isEqualTo(LifecycleStage.GROWTH);
        assertThat(analyze(stats(30, 30, 5), COSTS, new CategoryBenchmark(0, null), 30).lifecycleStage()).isEqualTo(LifecycleStage.DECLINE);
        assertThat(analyze(stats(30, 10, 10), COSTS, new CategoryBenchmark(0, null), 30).lifecycleStage()).isEqualTo(LifecycleStage.MATURE);
    }

    @Test
    void returnsNullScoresWhenFewerThanTwoCompetitionOrRiskMetricsExist() {
        List<AnalysisDailyStat> values = stats(30, 10, 20);
        AnalysisDailyStat latest = values.getLast();
        values.set(values.size() - 1, new AnalysisDailyStat(latest.statDate(), latest.price(), latest.salesVolume(), latest.videoCount(),
                latest.creatorCount(), null, null, null, null, null, null));

        AnalysisResult result = analyze(values, COSTS, new CategoryBenchmark(0, null), 30);
        assertThat(result.competitionScore()).isNull();
        assertThat(result.riskScore()).isNotNull();
        assertThat(result.selectionScore()).isNull();
    }

    @Test
    void keepsPricePressureAtZeroWhenPriceRisesAndCapsExtremeGrowthScores() {
        List<AnalysisDailyStat> values = stats(30, 0, 500);
        AnalysisDailyStat latest = values.getLast();
        values.set(0, new AnalysisDailyStat(values.getFirst().statDate(), bd("10"), values.getFirst().salesVolume(), 10L, 8L, 8L, 30L, bd("40"), bd("35"), bd("4"), bd("4.2")));
        values.set(values.size() - 1, new AnalysisDailyStat(latest.statDate(), bd("40"), latest.salesVolume(), latest.videoCount(),
                latest.creatorCount(), latest.shopCount(), latest.similarProductCount(), latest.top10ShopSalesShare(), latest.top10CreatorSalesShare(), latest.negativeReviewRate(), latest.rating()));

        AnalysisResult result = analyze(values, COSTS, new CategoryBenchmark(0, null), 30);
        assertThat(result.scoreDetails()).filteredOn(detail -> detail.metricCode().equals("PRICE_PRESSURE"))
                .extracting(ScoreDetail::normalizedScore).containsExactly(bd("0.00"));
        assertThat(result.scoreDetails()).filteredOn(detail -> detail.metricCode().equals("SALES_GROWTH_7D"))
                .extracting(ScoreDetail::normalizedScore).containsExactly(bd("100.00"));
    }

    @Test
    void clampsRatingRiskAtZeroForRatingsAboveTheRiskThreshold() {
        List<AnalysisDailyStat> values = stats(30, 10, 20);
        AnalysisDailyStat latest = values.getLast();
        values.set(values.size() - 1, new AnalysisDailyStat(latest.statDate(), latest.price(), latest.salesVolume(), latest.videoCount(),
                latest.creatorCount(), latest.shopCount(), latest.similarProductCount(), latest.top10ShopSalesShare(),
                latest.top10CreatorSalesShare(), latest.negativeReviewRate(), bd("5.00")));

        AnalysisResult result = analyze(values, COSTS, new CategoryBenchmark(0, null), 30);
        assertThat(result.scoreDetails()).filteredOn(detail -> detail.metricCode().equals("RATING_RISK"))
                .extracting(ScoreDetail::normalizedScore).containsExactly(bd("0.00"));
    }

    @Test
    void returnsNoSelectionWhenRiskHasOnlyOneValidMetricAfterOtherScoresSucceed() {
        List<AnalysisDailyStat> values = stats(30, 10, 20).stream()
                .map(stat -> new AnalysisDailyStat(stat.statDate(), stat.statDate().equals(ANALYSIS_DATE) ? bd("27") : null, stat.salesVolume(), stat.videoCount(), stat.creatorCount(),
                        stat.shopCount(), stat.similarProductCount(), stat.top10ShopSalesShare(), null, null, null))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        AnalysisResult result = analyze(values, COSTS, new CategoryBenchmark(0, null), 30);
        assertThat(result.trendScore()).isNotNull();
        assertThat(result.competitionScore()).isNotNull();
        assertThat(result.profitScore()).isNotNull();
        assertThat(result.riskScore()).isNull();
        assertThat(result.selectionScore()).isNull();
    }

    @Test
    void omitsProfitAndSelectionWhenNoCostProfileIsAvailable() {
        AnalysisResult result = analyze(stats(30, 10, 20), null, new CategoryBenchmark(0, null), 30);
        assertThat(result.profitCalculation()).isNull();
        assertThat(result.profitScore()).isNull();
        assertThat(result.selectionScore()).isNull();
    }

    @Test
    void handlesMissingLatestDateAndMissingListedDateAsSpecified() {
        List<AnalysisDailyStat> values = stats(14, 10, 20);
        values.removeLast();
        AnalysisResult missingLatest = algorithm.analyze(new AnalysisInput(1, "US", 2, "USD", null, ANALYSIS_DATE,
                SelectionV1Algorithm.VERSION, values, COSTS, new CategoryBenchmark(0, null)));
        assertThat(missingLatest.scoreStatus()).isEqualTo(ScoreStatus.DATA_INSUFFICIENT);
        assertThat(missingLatest.lifecycleStage()).isNull();
    }

    @Test
    void usesP75AboveOneHundredAndHandlesZeroTotalCost() {
        assertThat(new CategoryBenchmark(50, 240L).explosiveSalesThreshold()).isEqualTo(240L);
        assertThat(new CategoryBenchmark(50, null).explosiveSalesThreshold()).isEqualTo(100L);
        CostProfile zeroCost = new CostProfile("USD", bd("0"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"), bd("0"));
        assertThat(algorithm.profit(bd("10"), zeroCost).costProfitMargin()).isNull();
        assertThatThrownBy(() -> algorithm.profit(null, zeroCost)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculatesProfitWithPublishedFormulaAndRejectsNonPositiveSellingPrice() {
        ProfitCalculation profit = algorithm.profit(bd("29.99"), COSTS);

        assertThat(profit.estimatedProfit()).isEqualByComparingTo("9.32");
        assertThat(profit.estimatedProfitMargin()).isEqualByComparingTo("31.09");
        assertThatThrownBy(() -> algorithm.profit(BigDecimal.ZERO, COSTS)).isInstanceOf(IllegalArgumentException.class);
    }

    private AnalysisResult analyze(List<AnalysisDailyStat> stats, CostProfile costs, CategoryBenchmark benchmark, long ageDays) {
        return algorithm.analyze(new AnalysisInput(1, "US", 2, "USD", ANALYSIS_DATE.minusDays(ageDays), ANALYSIS_DATE,
                SelectionV1Algorithm.VERSION, stats, costs, benchmark));
    }

    private List<AnalysisDailyStat> stats(int days, long earlySales, long lateSales) {
        List<AnalysisDailyStat> values = new ArrayList<>();
        for (int index = 0; index < days; index++) {
            boolean late = index >= days - 7;
            values.add(new AnalysisDailyStat(
                    ANALYSIS_DATE.minusDays(days - index - 1),
                    index == 0 ? bd("30") : bd("27"),
                    late ? lateSales : earlySales,
                    10L + index, 8L + index, 8L, 30L, bd("40"), bd("35"), bd("4"), bd("4.2")
            ));
        }
        return values;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
