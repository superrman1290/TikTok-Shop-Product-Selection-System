package com.tiktokinsight.watchlist.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistItem(
        long productId, String title, String market, String currency, BigDecimal currentPrice,
        String imageUrl, BigDecimal selectionScore, String recommendation, Instant createdAt
) { }
