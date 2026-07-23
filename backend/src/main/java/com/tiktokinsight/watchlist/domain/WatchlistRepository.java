package com.tiktokinsight.watchlist.domain;

import com.tiktokinsight.common.api.PageResponse;
import java.time.Instant;

public interface WatchlistRepository {
    boolean productExists(long productId);
    boolean add(long userId, long productId, Instant now);
    boolean remove(long userId, long productId);
    PageResponse<WatchlistItem> findByUser(long userId, String market, int page, int pageSize);
    long countByUser(long userId);
}
