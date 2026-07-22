package com.tiktokinsight.analysis.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Pure implementation of the published selection-v1.0 contract. */
public final class SelectionV1Algorithm {

    public static final String VERSION = "selection-v1.0";
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final MathContext MATH = new MathContext(12, RoundingMode.HALF_UP);
    private static final BigDecimal TREND_PARENT_WEIGHT = new BigDecimal("0.35");
    private static final BigDecimal COMPETITION_PARENT_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal PROFIT_PARENT_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal RISK_PARENT_WEIGHT = new BigDecimal("0.15");

    public AnalysisResult analyze(AnalysisInput input) {
        if (!VERSION.equals(input.algorithmVersion())) {
            throw new IllegalArgumentException("Unsupported algorithm version: " + input.algorithmVersion());
        }
        List<AnalysisDailyStat> continuousStats = continuousStatsEndingAt(input.dailyStats(), input.analysisDate());
        ScoreStatus status = scoreStatus(continuousStats.size());
        AnalysisDailyStat latest = continuousStats.isEmpty() ? null : continuousStats.getLast();
        List<ScoreDetail> details = new ArrayList<>();

        Long salesVolume7d = null;
        BigDecimal salesGrowth7d = null;
        BigDecimal salesGrowth30d = null;
        BigDecimal videoGrowth7d = null;
        BigDecimal creatorGrowth7d = null;
        BigDecimal trendScore = null;
        BigDecimal competitionScore = null;
        BigDecimal riskScore = null;

        if (status != ScoreStatus.DATA_INSUFFICIENT) {
            salesVolume7d = sumSales(continuousStats.subList(continuousStats.size() - 7, continuousStats.size()));
            long previousSalesVolume7d = sumSales(continuousStats.subList(continuousStats.size() - 14, continuousStats.size() - 7));
            salesGrowth7d = growth(salesVolume7d, previousSalesVolume7d);
            videoGrowth7d = growth(value(number(latest.videoCount())), value(number(continuousStats.get(continuousStats.size() - 8).videoCount())));
            creatorGrowth7d = growth(value(number(latest.creatorCount())), value(number(continuousStats.get(continuousStats.size() - 8).creatorCount())));
            if (status == ScoreStatus.COMPLETE) {
                long latestSalesVolume15d = sumSales(continuousStats.subList(continuousStats.size() - 15, continuousStats.size()));
                long previousSalesVolume15d = sumSales(continuousStats.subList(continuousStats.size() - 30, continuousStats.size() - 15));
                salesGrowth30d = growth(latestSalesVolume15d, previousSalesVolume15d);
            }
            trendScore = trendScore(salesGrowth7d, salesGrowth30d, videoGrowth7d, creatorGrowth7d, status, details);
            competitionScore = competitionScore(continuousStats, status, details);
            riskScore = riskScore(continuousStats, details);
        }

        ProfitCalculation profit = latest == null || input.costProfile() == null ? null : profit(latest.price(), input.costProfile());
        BigDecimal profitScore = profit == null ? null : profitScore(profit.estimatedProfitMargin());
        if (profitScore != null) {
            details.add(detail("PROFIT_MARGIN", "预计利润率", profit.estimatedProfitMargin(), profitScore,
                    ONE, PROFIT_PARENT_WEIGHT, "预计利润率为" + text(profit.estimatedProfitMargin()) + "%"));
        }

        BigDecimal selectionScore = selectionScore(trendScore, competitionScore, profitScore, riskScore);
        LifecycleStage lifecycle = lifecycle(input, salesVolume7d, salesGrowth7d, salesGrowth30d);
        Recommendation recommendation = recommendationForScore(selectionScore);
        appendSelectionDetails(details, competitionScore, riskScore);
        List<String> reasons = selectionScore == null ? List.of() : messages(details, true);
        List<String> risks = selectionScore == null ? List.of() : messages(details, false);
        return new AnalysisResult(
                input.productId(), input.market(), input.categoryId(), input.analysisDate(), input.algorithmVersion(),
                status, salesVolume7d, output(salesGrowth7d), output(salesGrowth30d), output(videoGrowth7d), output(creatorGrowth7d),
                output(trendScore), output(competitionScore), profit, output(profitScore), output(riskScore), output(selectionScore),
                lifecycle, recommendation, details, reasons, risks
        );
    }

    public ProfitCalculation profit(BigDecimal sellingPrice, CostProfile costs) {
        if (sellingPrice == null || sellingPrice.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("sellingPrice must be greater than zero");
        }
        BigDecimal platformCommission = percentage(sellingPrice, costs.platformCommissionRate());
        BigDecimal paymentFee = percentage(sellingPrice, costs.paymentFeeRate());
        BigDecimal advertisingCost = percentage(sellingPrice, costs.advertisingCostRate());
        BigDecimal refundLoss = percentage(sellingPrice, costs.refundLossRate());
        BigDecimal totalCost = value(costs.purchaseCost()).add(value(costs.domesticShippingCost())).add(value(costs.internationalShippingCost()))
                .add(platformCommission).add(paymentFee).add(advertisingCost).add(refundLoss).add(value(costs.otherCost()));
        BigDecimal estimatedProfit = sellingPrice.subtract(totalCost);
        BigDecimal profitMargin = estimatedProfit.divide(sellingPrice, 6, RoundingMode.HALF_UP).multiply(HUNDRED);
        BigDecimal costProfitMargin = totalCost.compareTo(ZERO) == 0 ? null
                : estimatedProfit.divide(totalCost, 6, RoundingMode.HALF_UP).multiply(HUNDRED);
        return new ProfitCalculation(output(sellingPrice), output(platformCommission), output(paymentFee), output(advertisingCost),
                output(refundLoss), output(totalCost), output(estimatedProfit), output(profitMargin), output(costProfitMargin));
    }

    private BigDecimal trendScore(
            BigDecimal sales7d, BigDecimal sales30d, BigDecimal video7d, BigDecimal creator7d,
            ScoreStatus status, List<ScoreDetail> details
    ) {
        List<WeightedMetric> metrics = new ArrayList<>();
        metrics.add(metric("SALES_GROWTH_7D", "近7日销量增长率", sales7d, growthScore(sales7d),
                status == ScoreStatus.COMPLETE ? "0.45" : "0.60", "最近7日销量较前7日增长" + text(sales7d) + "%"));
        if (status == ScoreStatus.COMPLETE) {
            metrics.add(metric("SALES_GROWTH_30D", "近30日销量增长率", sales30d, growthScore(sales30d), "0.25",
                    "最近15日销量较前15日增长" + text(sales30d) + "%"));
        }
        metrics.add(metric("VIDEO_GROWTH_7D", "近7日带货视频增长率", video7d, growthScore(video7d),
                status == ScoreStatus.COMPLETE ? "0.15" : "0.20", "近7日带货视频数量增长" + text(video7d) + "%"));
        metrics.add(metric("CREATOR_GROWTH_7D", "近7日达人成长率", creator7d, growthScore(creator7d),
                status == ScoreStatus.COMPLETE ? "0.15" : "0.20", "近7日相关达人数量增长" + text(creator7d) + "%"));
        return weightedScore(metrics, TREND_PARENT_WEIGHT, details);
    }

    private BigDecimal competitionScore(List<AnalysisDailyStat> stats, ScoreStatus status, List<ScoreDetail> details) {
        AnalysisDailyStat latest = stats.getLast();
        BigDecimal priceDrop = status == ScoreStatus.COMPLETE
                ? priceDropRate(stats.get(stats.size() - 30).price(), latest.price()) : null;
        return weightedScore(List.of(
                metric("SHOP_INTENSITY", "同类活跃店铺数", number(latest.shopCount()), scale(number(latest.shopCount()), 1, 30), "0.35",
                        "当前同类活跃店铺数量为" + text(number(latest.shopCount()))),
                metric("SIMILAR_PRODUCT_INTENSITY", "同类相似商品数", number(latest.similarProductCount()), scale(number(latest.similarProductCount()), 5, 100), "0.30",
                        "当前同类相似商品数量为" + text(number(latest.similarProductCount()))),
                metric("PRICE_PRESSURE", "30日价格下降率", priceDrop, scale(priceDrop, 0, 30), "0.15",
                        "最近30日价格下降" + text(priceDrop) + "%"),
                metric("SHOP_CONCENTRATION", "前10店铺销量占比", latest.top10ShopSalesShare(), scale(latest.top10ShopSalesShare(), 30, 90), "0.20",
                        "前10店铺销量占比为" + text(latest.top10ShopSalesShare()) + "%")
        ), COMPETITION_PARENT_WEIGHT, details);
    }

    private BigDecimal riskScore(List<AnalysisDailyStat> stats, List<ScoreDetail> details) {
        AnalysisDailyStat latest = stats.getLast();
        int windowStart = Math.max(0, stats.size() - 30);
        List<AnalysisDailyStat> window = stats.subList(windowStart, stats.size());
        BigDecimal priceVolatility = volatility(window, AnalysisDailyStat::price);
        BigDecimal salesVolatility = volatility(window, stat -> number(stat.salesVolume()));
        BigDecimal ratingRisk = latest.rating() == null ? null : clamp(new BigDecimal("4.5").subtract(latest.rating())
                .divide(new BigDecimal("1.5"), 6, RoundingMode.HALF_UP).multiply(HUNDRED), ZERO, HUNDRED);
        return weightedScore(List.of(
                metric("PRICE_VOLATILITY", "价格波动率", priceVolatility, scale(priceVolatility, 0, 20), "0.25",
                        "最近30日价格波动率为" + text(priceVolatility) + "%"),
                metric("SALES_VOLATILITY", "销量波动率", salesVolatility, scale(salesVolatility, 0, 100), "0.25",
                        "最近30日销量波动率为" + text(salesVolatility) + "%"),
                metric("CREATOR_CONCENTRATION", "前10达人销量占比", latest.top10CreatorSalesShare(), scale(latest.top10CreatorSalesShare(), 30, 90), "0.20",
                        "前10达人销量占比为" + text(latest.top10CreatorSalesShare()) + "%"),
                metric("RATING_RISK", "商品评分风险", latest.rating(), ratingRisk, "0.15",
                        "当前商品评分为" + text(latest.rating())),
                metric("NEGATIVE_REVIEW_RISK", "负面评价比例", latest.negativeReviewRate(), scale(latest.negativeReviewRate(), 0, 20), "0.15",
                        "当前负面评价比例为" + text(latest.negativeReviewRate()) + "%")
        ), RISK_PARENT_WEIGHT, details);
    }

    private BigDecimal weightedScore(List<WeightedMetric> candidates, BigDecimal parentWeight, List<ScoreDetail> details) {
        List<WeightedMetric> metrics = candidates.stream().filter(WeightedMetric::isValid).toList();
        if (metrics.size() < 2) {
            return null;
        }
        BigDecimal totalWeight = metrics.stream().map(WeightedMetric::weight).reduce(ZERO, BigDecimal::add);
        BigDecimal score = ZERO;
        for (WeightedMetric metric : metrics) {
            BigDecimal normalizedWeight = metric.weight().divide(totalWeight, 6, RoundingMode.HALF_UP);
            score = score.add(metric.normalizedScore().multiply(normalizedWeight));
            details.add(detail(metric.code(), metric.name(), metric.rawValue(), metric.normalizedScore(), normalizedWeight,
                    parentWeight, metric.description()));
        }
        return output(score);
    }

    private void appendSelectionDetails(List<ScoreDetail> details, BigDecimal competitionScore, BigDecimal riskScore) {
        if (competitionScore != null) {
            BigDecimal score = HUNDRED.subtract(competitionScore);
            details.add(detail("LOW_COMPETITION", "低竞争优势", competitionScore, score, ONE, COMPETITION_PARENT_WEIGHT,
                    "当前竞争评分为" + text(competitionScore) + "，竞争强度相对较低"));
        }
        if (riskScore != null) {
            BigDecimal score = HUNDRED.subtract(riskScore);
            details.add(detail("LOW_RISK", "低风险优势", riskScore, score, ONE, RISK_PARENT_WEIGHT,
                    "当前风险评分为" + text(riskScore) + "，经营风险相对较低"));
        }
    }

    private List<String> messages(List<ScoreDetail> details, boolean reason) {
        List<String> includedCodes = reason
                ? List.of("SALES_GROWTH_7D", "SALES_GROWTH_30D", "VIDEO_GROWTH_7D", "CREATOR_GROWTH_7D", "PROFIT_MARGIN", "LOW_COMPETITION", "LOW_RISK")
                : List.of("SHOP_INTENSITY", "SIMILAR_PRODUCT_INTENSITY", "PRICE_PRESSURE", "SHOP_CONCENTRATION", "PRICE_VOLATILITY", "SALES_VOLATILITY", "CREATOR_CONCENTRATION", "RATING_RISK", "NEGATIVE_REVIEW_RISK");
        return details.stream()
                .filter(detail -> includedCodes.contains(detail.metricCode()) && detail.normalizedScore().compareTo(new BigDecimal("60")) >= 0)
                .sorted(Comparator.comparing(ScoreDetail::overallContribution).reversed().thenComparing(ScoreDetail::metricCode))
                .limit(5)
                .map(ScoreDetail::description)
                .toList();
    }

    private LifecycleStage lifecycle(AnalysisInput input, Long sales7d, BigDecimal salesGrowth7d, BigDecimal salesGrowth30d) {
        if (input.listedDate() != null && ChronoUnit.DAYS.between(input.listedDate(), input.analysisDate()) <= 14) {
            return LifecycleStage.NEW;
        }
        if (sales7d == null || salesGrowth7d == null) {
            return null;
        }
        long threshold = input.benchmark() == null ? 100 : input.benchmark().explosiveSalesThreshold();
        if (salesGrowth7d.compareTo(new BigDecimal("80")) >= 0 && sales7d >= threshold) {
            return LifecycleStage.EXPLOSIVE;
        }
        if (salesGrowth7d.compareTo(new BigDecimal("20")) >= 0
                && (salesGrowth30d == null || salesGrowth30d.compareTo(new BigDecimal("10")) >= 0)) {
            return LifecycleStage.GROWTH;
        }
        if (salesGrowth7d.compareTo(new BigDecimal("-20")) <= 0
                && (salesGrowth30d == null || salesGrowth30d.compareTo(new BigDecimal("-10")) <= 0)) {
            return LifecycleStage.DECLINE;
        }
        return LifecycleStage.MATURE;
    }

    public Recommendation recommendationForScore(BigDecimal score) {
        if (score == null) return Recommendation.DATA_INSUFFICIENT;
        if (score.compareTo(new BigDecimal("75")) >= 0) return Recommendation.RECOMMENDED;
        if (score.compareTo(new BigDecimal("60")) >= 0) return Recommendation.WATCH;
        return Recommendation.NOT_RECOMMENDED;
    }

    private BigDecimal selectionScore(BigDecimal trend, BigDecimal competition, BigDecimal profit, BigDecimal risk) {
        if (trend == null || competition == null || profit == null || risk == null) return null;
        return output(trend.multiply(TREND_PARENT_WEIGHT)
                .add(HUNDRED.subtract(competition).multiply(COMPETITION_PARENT_WEIGHT))
                .add(profit.multiply(PROFIT_PARENT_WEIGHT))
                .add(HUNDRED.subtract(risk).multiply(RISK_PARENT_WEIGHT)));
    }

    private BigDecimal profitScore(BigDecimal profitMargin) {
        return profitMargin.compareTo(ZERO) <= 0 ? ZERO : scale(profitMargin, 0, 40);
    }

    private BigDecimal priceDropRate(BigDecimal first, BigDecimal latest) {
        if (first == null || latest == null) return null;
        return max(ZERO, first.subtract(latest).divide(max(first, new BigDecimal("0.01")), 6, RoundingMode.HALF_UP).multiply(HUNDRED));
    }

    private BigDecimal volatility(List<AnalysisDailyStat> stats, Function<AnalysisDailyStat, BigDecimal> selector) {
        List<BigDecimal> values = stats.stream().map(selector).toList();
        if (values.stream().anyMatch(value -> value == null) || values.isEmpty()) return null;
        BigDecimal average = values.stream().reduce(ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        if (average.compareTo(ZERO) == 0) return null;
        BigDecimal variance = values.stream().map(value -> value.subtract(average).pow(2)).reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);
        return variance.sqrt(MATH).divide(average, 6, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private ScoreStatus scoreStatus(int continuousDays) {
        if (continuousDays < 14) return ScoreStatus.DATA_INSUFFICIENT;
        return continuousDays < 30 ? ScoreStatus.PARTIAL : ScoreStatus.COMPLETE;
    }

    private List<AnalysisDailyStat> continuousStatsEndingAt(List<AnalysisDailyStat> stats, LocalDate analysisDate) {
        Map<LocalDate, AnalysisDailyStat> byDate = new HashMap<>();
        stats.stream().filter(stat -> !stat.statDate().isAfter(analysisDate)).forEach(stat -> byDate.put(stat.statDate(), stat));
        List<AnalysisDailyStat> reverse = new ArrayList<>();
        for (LocalDate date = analysisDate; byDate.containsKey(date); date = date.minusDays(1)) reverse.add(byDate.get(date));
        java.util.Collections.reverse(reverse);
        return reverse;
    }

    private long sumSales(List<AnalysisDailyStat> stats) {
        long total = 0;
        for (AnalysisDailyStat stat : stats) {
            if (stat.salesVolume() == null) throw new IllegalArgumentException("salesVolume is required for continuous data");
            total = Math.addExact(total, stat.salesVolume());
        }
        return total;
    }

    private BigDecimal growth(long latest, long previous) {
        return growth(BigDecimal.valueOf(latest), BigDecimal.valueOf(previous));
    }

    private BigDecimal growth(BigDecimal latest, BigDecimal previous) {
        return latest.subtract(previous).divide(max(previous, ONE), 6, RoundingMode.HALF_UP).multiply(HUNDRED);
    }

    private BigDecimal growthScore(BigDecimal value) {
        return scale(value, -50, 100);
    }

    private BigDecimal scale(BigDecimal value, int min, int max) {
        if (value == null) return null;
        return clamp(value.subtract(BigDecimal.valueOf(min)).divide(BigDecimal.valueOf(max - min), 6, RoundingMode.HALF_UP).multiply(HUNDRED), ZERO, HUNDRED);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        return max(min, value.min(max));
    }

    private BigDecimal percentage(BigDecimal sellingPrice, BigDecimal rate) {
        return sellingPrice.multiply(value(rate)).divide(HUNDRED, 6, RoundingMode.HALF_UP);
    }

    private ScoreDetail detail(String code, String name, BigDecimal rawValue, BigDecimal normalizedScore, BigDecimal weight,
                                BigDecimal parentWeight, String description) {
        BigDecimal contribution = normalizedScore.multiply(weight).multiply(parentWeight);
        return new ScoreDetail(code, name, output(rawValue), output(normalizedScore), output(weight), output(parentWeight), output(contribution), description);
    }

    private WeightedMetric metric(String code, String name, BigDecimal rawValue, BigDecimal normalizedScore, String weight, String description) {
        return new WeightedMetric(code, name, rawValue, normalizedScore, new BigDecimal(weight), description);
    }

    private BigDecimal number(Long value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal output(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal first, BigDecimal second) {
        return first.max(second);
    }

    private String text(BigDecimal value) {
        return value == null ? "-" : output(value).toPlainString();
    }

    private record WeightedMetric(String code, String name, BigDecimal rawValue, BigDecimal normalizedScore,
                                  BigDecimal weight, String description) {
        boolean isValid() {
            return rawValue != null && normalizedScore != null;
        }
    }
}
