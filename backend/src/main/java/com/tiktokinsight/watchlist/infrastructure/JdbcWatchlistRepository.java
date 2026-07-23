package com.tiktokinsight.watchlist.infrastructure;

import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.watchlist.domain.WatchlistItem;
import com.tiktokinsight.watchlist.domain.WatchlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcWatchlistRepository implements WatchlistRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcWatchlistRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public boolean productExists(long productId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE id = :productId AND status = 'ACTIVE'", Map.of("productId", productId), Integer.class);
        return count != null && count > 0;
    }

    @Override public boolean add(long userId, long productId, Instant now) {
        try {
            return jdbc.update("INSERT INTO watchlist (user_id, product_id, created_at) VALUES (:userId, :productId, :now)",
                    new MapSqlParameterSource().addValue("userId", userId).addValue("productId", productId).addValue("now", now)) == 1;
        } catch (DuplicateKeyException exception) { return false; }
    }

    @Override public boolean remove(long userId, long productId) {
        return jdbc.update("DELETE FROM watchlist WHERE user_id = :userId AND product_id = :productId", Map.of("userId", userId, "productId", productId)) > 0;
    }

    @Override public PageResponse<WatchlistItem> findByUser(long userId, String market, int page, int pageSize) {
        String filter = market == null ? "" : " AND p.market = :market";
        Map<String, Object> parameters = new HashMap<>(); parameters.put("userId", userId); if (market != null) parameters.put("market", market);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM watchlist w JOIN product p ON p.id = w.product_id WHERE w.user_id = :userId" + filter, parameters, Long.class);
        parameters.put("limit", pageSize); parameters.put("offset", (page - 1) * pageSize);
        List<WatchlistItem> items = jdbc.query("""
                SELECT p.id AS product_id, p.title, p.market, p.currency, p.current_price, p.image_url, w.created_at,
                       snapshot.selection_score, snapshot.recommendation
                  FROM watchlist w JOIN product p ON p.id = w.product_id
                  LEFT JOIN product_analysis_snapshot snapshot ON snapshot.id = (
                      SELECT s.id FROM product_analysis_snapshot s WHERE s.product_id = p.id
                      ORDER BY s.analysis_date DESC, s.calculated_at DESC LIMIT 1)
                 WHERE w.user_id = :userId
                """ + filter + " ORDER BY w.created_at DESC, w.id DESC LIMIT :limit OFFSET :offset", parameters,
                (rs, row) -> new WatchlistItem(rs.getLong("product_id"), rs.getString("title"), rs.getString("market"), rs.getString("currency"),
                        rs.getBigDecimal("current_price"), rs.getString("image_url"), rs.getBigDecimal("selection_score"), rs.getString("recommendation"), rs.getTimestamp("created_at").toInstant()));
        return new PageResponse<>(page, pageSize, total == null ? 0 : total, items);
    }

    @Override public long countByUser(long userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM watchlist WHERE user_id = :userId", Map.of("userId", userId), Long.class);
        return count == null ? 0 : count;
    }
}
