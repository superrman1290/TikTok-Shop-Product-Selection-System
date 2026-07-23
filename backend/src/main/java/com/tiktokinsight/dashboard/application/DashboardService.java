package com.tiktokinsight.dashboard.application;

import com.tiktokinsight.alert.application.AlertService;
import com.tiktokinsight.dashboard.domain.DashboardRepository;
import com.tiktokinsight.dashboard.domain.UserDashboard;
import com.tiktokinsight.watchlist.application.WatchlistService;
import com.tiktokinsight.datasource.SupportedMarket;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

@Service
public class DashboardService {
    private final DashboardRepository repository; private final WatchlistService watchlistService; private final AlertService alertService; private final Clock clock; private final String activeVersion;
    public DashboardService(DashboardRepository repository, WatchlistService watchlistService, AlertService alertService, Clock clock, @Value("${app.analysis.active-version:selection-v1.0}") String activeVersion) { this.repository = repository; this.watchlistService = watchlistService; this.alertService = alertService; this.clock = clock; this.activeVersion = activeVersion; }
    public UserDashboard get(long userId, String requestedMarket) {
        String market = requestedMarket == null || requestedMarket.isBlank() ? "US" : requestedMarket.trim().toUpperCase();
        if (SupportedMarket.find(market).isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.MARKET_UNSUPPORTED);
        LocalDate since = LocalDate.now(clock).minusDays(7);
        return new UserDashboard(market, repository.activeProductCount(market), repository.newProductCount(market, since), repository.recommendedProductCount(market, activeVersion), watchlistService.count(userId), alertService.unreadCount(userId), repository.topSalesGrowth(market, activeVersion, 10), repository.topSelectionScore(market, activeVersion, 10));
    }
}
