package com.tiktokinsight.watchlist.application;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.watchlist.domain.WatchlistItem;
import com.tiktokinsight.watchlist.domain.WatchlistRepository;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {
    private final WatchlistRepository repository;
    private final Clock clock;

    public WatchlistService(WatchlistRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PageResponse<WatchlistItem> list(long userId, String market, int page, int pageSize) {
        validatePaging(page, pageSize);
        return repository.findByUser(userId, normalizeMarket(market), page, pageSize);
    }

    public void add(long userId, long productId) {
        if (!repository.productExists(productId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!repository.add(userId, productId, clock.instant())) {
            throw new ApiException(HttpStatus.CONFLICT, ApiErrorCode.PRODUCT_ALREADY_WATCHLISTED);
        }
    }

    public void remove(long userId, long productId) {
        repository.remove(userId, productId);
    }

    public long count(long userId) { return repository.countByUser(userId); }

    private void validatePaging(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }

    private String normalizeMarket(String market) {
        return market == null || market.isBlank() ? null : market.trim().toUpperCase();
    }
}
