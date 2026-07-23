package com.tiktokinsight.dashboard.domain;

import java.util.List;

public record UserDashboard(String market, long productCount, long newProducts7d, long recommendedProducts,
                            long watchlistProductCount, long unreadAlertCount,
                            List<DashboardProduct> topSalesGrowthProducts, List<DashboardProduct> topSelectionScoreProducts) { }
