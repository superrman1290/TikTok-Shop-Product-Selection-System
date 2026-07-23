package com.tiktokinsight.alert.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AlertEvaluationTarget(long ruleId, long userId, long productId, LocalDate statDate,
                                    BigDecimal salesGrowthRate7d, BigDecimal currentPrice, BigDecimal priceSevenDaysAgo,
                                    BigDecimal competitionScore, BigDecimal previousCompetitionScore,
                                    BigDecimal selectionScore, BigDecimal previousSelectionScore,
                                    BigDecimal salesGrowth7dThreshold, BigDecimal priceDrop7dThreshold,
                                    BigDecimal competitionScoreIncreaseThreshold, BigDecimal selectionScoreDropThreshold) { }
