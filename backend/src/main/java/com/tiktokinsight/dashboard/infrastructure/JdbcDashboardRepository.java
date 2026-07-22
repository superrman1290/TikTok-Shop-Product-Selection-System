package com.tiktokinsight.dashboard.infrastructure;

import com.tiktokinsight.dashboard.domain.DashboardProduct;
import com.tiktokinsight.dashboard.domain.DashboardRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDashboardRepository implements DashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcDashboardRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public long activeProductCount(String market) { return count("SELECT COUNT(*) FROM product WHERE market = :market AND status = 'ACTIVE'", Map.of("market", market)); }
    @Override public long newProductCount(String market, LocalDate since) { return count("SELECT COUNT(*) FROM product WHERE market = :market AND status = 'ACTIVE' AND listed_at >= :since", Map.of("market", market, "since", since.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())); }
    @Override public long recommendedProductCount(String market, String algorithmVersion) { return count("""
            SELECT COUNT(*) FROM product p JOIN product_analysis_snapshot s ON s.product_id = p.id AND s.analysis_date = p.latest_stat_date AND s.algorithm_version = :version
             WHERE p.market = :market AND p.status = 'ACTIVE' AND s.recommendation = 'RECOMMENDED'
            """, Map.of("market", market, "version", algorithmVersion)); }
    @Override public List<DashboardProduct> topSalesGrowth(String market, String algorithmVersion, int limit) { return products(market, algorithmVersion, limit, "s.sales_growth_rate_7d DESC"); }
    @Override public List<DashboardProduct> topSelectionScore(String market, String algorithmVersion, int limit) { return products(market, algorithmVersion, limit, "s.selection_score DESC"); }
    private List<DashboardProduct> products(String market, String algorithmVersion, int limit, String order) { return jdbc.query("""
            SELECT p.id, p.title, p.market, p.currency, p.current_price, s.sales_growth_rate_7d, s.selection_score, s.recommendation
              FROM product p JOIN product_analysis_snapshot s ON s.product_id = p.id AND s.analysis_date = p.latest_stat_date AND s.algorithm_version = :version
             WHERE p.market = :market AND p.status = 'ACTIVE' AND s.score_status <> 'DATA_INSUFFICIENT'
             ORDER BY""" + " " + order + ", p.id ASC LIMIT :limit", Map.of("market", market, "version", algorithmVersion, "limit", limit), (rs, row) -> new DashboardProduct(rs.getLong("id"), rs.getString("title"), rs.getString("market"), rs.getString("currency"), rs.getBigDecimal("current_price"), rs.getBigDecimal("sales_growth_rate_7d"), rs.getBigDecimal("selection_score"), rs.getString("recommendation"))); }
    private long count(String sql, Map<String, Object> parameters) { Long value = jdbc.queryForObject(sql, parameters, Long.class); return value == null ? 0 : value; }
}
