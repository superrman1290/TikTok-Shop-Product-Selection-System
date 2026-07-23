package com.tiktokinsight.dashboard.domain;

import java.math.BigDecimal;

public record DashboardProduct(long productId, String title, String market, String currency, BigDecimal currentPrice,
                               BigDecimal salesGrowthRate7d, BigDecimal selectionScore, String recommendation) { }
