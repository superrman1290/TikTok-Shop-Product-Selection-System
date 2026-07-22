package com.tiktokinsight.dashboard.domain;

import java.time.LocalDate;
import java.util.List;

public interface DashboardRepository {
    long activeProductCount(String market);
    long newProductCount(String market, LocalDate since);
    long recommendedProductCount(String market, String algorithmVersion);
    List<DashboardProduct> topSalesGrowth(String market, String algorithmVersion, int limit);
    List<DashboardProduct> topSelectionScore(String market, String algorithmVersion, int limit);
}
