package com.tiktokinsight.product.infrastructure;

import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.product.domain.ProductAdministrationUpdate;
import com.tiktokinsight.product.domain.ProductCatalogRepository;
import com.tiktokinsight.product.domain.ProductDailyStat;
import com.tiktokinsight.product.domain.ProductDetail;
import com.tiktokinsight.product.domain.ProductQuery;
import com.tiktokinsight.product.domain.ProductStatus;
import com.tiktokinsight.product.domain.ProductSummary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcProductCatalogRepository implements ProductCatalogRepository {

    private static final String BASE_SELECT = """
            SELECT p.id, p.external_product_id, p.platform, p.market, p.title,
                   p.category_id, c.external_id AS category_external_id, c.name AS category_name,
                   p.shop_id, s.external_id AS shop_external_id, s.name AS shop_name,
                   p.currency, p.current_price, p.original_price, p.image_url, p.product_url,
                   p.rating, p.review_count, p.listed_at, p.collected_at, p.latest_stat_date, p.status
              FROM product p
              JOIN category c ON c.id = p.category_id
              JOIN shop s ON s.id = p.shop_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProductCatalogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResponse<ProductSummary> findProducts(ProductQuery query) {
        var where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();
        appendFilters(query, where, parameters);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM product p" + where,
                parameters,
                Long.class
        );
        parameters.put("limit", query.pageSize());
        parameters.put("offset", query.offset());
        String sql = BASE_SELECT + where
                + " ORDER BY " + query.sort().column() + " " + query.direction().name()
                + ", p.id " + query.direction().name()
                + " LIMIT :limit OFFSET :offset";
        List<ProductSummary> items = jdbc.query(sql, parameters, (resultSet, rowNumber) -> summary(resultSet));
        return new PageResponse<>(query.page(), query.pageSize(), total == null ? 0 : total, items);
    }

    @Override
    public Optional<ProductDetail> findById(long productId, boolean includeInactive) {
        String sql = BASE_SELECT + " WHERE p.id = :id" + (includeInactive ? "" : " AND p.status = 'ACTIVE'");
        List<ProductDetail> results = jdbc.query(sql, Map.of("id", productId), (resultSet, rowNumber) -> detail(resultSet));
        return results.stream().findFirst();
    }

    @Override
    public List<ProductDailyStat> findStats(long productId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT stat_date, price, sales_volume, sales_amount, total_sales_volume,
                       video_count, creator_count, shop_count, similar_product_count,
                       top10_shop_sales_share, top10_creator_sales_share, negative_review_rate,
                       review_count, rating
                  FROM product_daily_stat
                 WHERE product_id = :productId
                """);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("productId", productId);
        if (startDate != null) {
            sql.append(" AND stat_date >= :startDate");
            parameters.put("startDate", startDate);
        }
        if (endDate != null) {
            sql.append(" AND stat_date <= :endDate");
            parameters.put("endDate", endDate);
        }
        sql.append(" ORDER BY stat_date ASC LIMIT 366");
        return jdbc.query(sql.toString(), parameters, (resultSet, rowNumber) -> new ProductDailyStat(
                resultSet.getObject("stat_date", LocalDate.class),
                resultSet.getBigDecimal("price"),
                resultSet.getLong("sales_volume"),
                resultSet.getBigDecimal("sales_amount"),
                resultSet.getLong("total_sales_volume"),
                resultSet.getLong("video_count"),
                resultSet.getLong("creator_count"),
                resultSet.getLong("shop_count"),
                resultSet.getLong("similar_product_count"),
                resultSet.getBigDecimal("top10_shop_sales_share"),
                resultSet.getBigDecimal("top10_creator_sales_share"),
                resultSet.getBigDecimal("negative_review_rate"),
                resultSet.getLong("review_count"),
                resultSet.getBigDecimal("rating")
        ));
    }

    @Override
    @Transactional
    public Optional<ProductDetail> update(long productId, ProductAdministrationUpdate update) {
        int updated = jdbc.update("""
                UPDATE product
                   SET title = :title,
                       current_price = :currentPrice,
                       original_price = :originalPrice,
                       image_url = :imageUrl,
                       product_url = :productUrl,
                       status = :status,
                       updated_at = UTC_TIMESTAMP(6)
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", productId)
                .addValue("title", update.title())
                .addValue("currentPrice", update.currentPrice())
                .addValue("originalPrice", update.originalPrice())
                .addValue("imageUrl", update.imageUrl())
                .addValue("productUrl", update.productUrl())
                .addValue("status", update.status().name()));
        return updated == 0 ? Optional.empty() : findById(productId, true);
    }

    private void appendFilters(ProductQuery query, StringBuilder where, Map<String, Object> parameters) {
        if (query.market() != null) {
            where.append(" AND p.market = :market");
            parameters.put("market", query.market());
        }
        if (query.categoryId() != null) {
            where.append(" AND p.category_id = :categoryId");
            parameters.put("categoryId", query.categoryId());
        }
        if (query.keyword() != null) {
            where.append(" AND p.title LIKE :keyword");
            parameters.put("keyword", "%" + query.keyword() + "%");
        }
        if (query.status() != null) {
            where.append(" AND p.status = :status");
            parameters.put("status", query.status().name());
        }
        if (query.minPrice() != null) { where.append(" AND p.current_price >= :minPrice"); parameters.put("minPrice", query.minPrice()); }
        if (query.maxPrice() != null) { where.append(" AND p.current_price <= :maxPrice"); parameters.put("maxPrice", query.maxPrice()); }
        if (query.lifecycleStage() != null || query.recommendation() != null || query.minSelectionScore() != null || query.maxSelectionScore() != null) {
            where.append(" AND EXISTS (SELECT 1 FROM product_analysis_snapshot snapshot WHERE snapshot.product_id = p.id AND snapshot.analysis_date = p.latest_stat_date AND snapshot.algorithm_version = :algorithmVersion");
            parameters.put("algorithmVersion", query.algorithmVersion());
            if (query.lifecycleStage() != null) { where.append(" AND snapshot.lifecycle_stage = :lifecycleStage"); parameters.put("lifecycleStage", query.lifecycleStage()); }
            if (query.recommendation() != null) { where.append(" AND snapshot.recommendation = :recommendation"); parameters.put("recommendation", query.recommendation()); }
            if (query.minSelectionScore() != null) { where.append(" AND snapshot.selection_score >= :minSelectionScore"); parameters.put("minSelectionScore", query.minSelectionScore()); }
            if (query.maxSelectionScore() != null) { where.append(" AND snapshot.selection_score <= :maxSelectionScore"); parameters.put("maxSelectionScore", query.maxSelectionScore()); }
            where.append(")");
        }
    }

    private ProductSummary summary(ResultSet resultSet) throws SQLException {
        return new ProductSummary(
                resultSet.getLong("id"),
                resultSet.getString("external_product_id"),
                resultSet.getString("platform"),
                resultSet.getString("market"),
                resultSet.getString("title"),
                resultSet.getLong("category_id"),
                resultSet.getString("category_name"),
                resultSet.getLong("shop_id"),
                resultSet.getString("shop_name"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("current_price"),
                resultSet.getBigDecimal("original_price"),
                resultSet.getString("image_url"),
                resultSet.getString("product_url"),
                resultSet.getBigDecimal("rating"),
                resultSet.getLong("review_count"),
                instant(resultSet, "listed_at"),
                instant(resultSet, "collected_at"),
                ProductStatus.valueOf(resultSet.getString("status"))
        );
    }

    private ProductDetail detail(ResultSet resultSet) throws SQLException {
        LocalDate latestStatDate = resultSet.getObject("latest_stat_date", LocalDate.class);
        return new ProductDetail(
                resultSet.getLong("id"),
                resultSet.getString("external_product_id"),
                resultSet.getString("platform"),
                resultSet.getString("market"),
                resultSet.getString("title"),
                resultSet.getLong("category_id"),
                resultSet.getString("category_external_id"),
                resultSet.getString("category_name"),
                resultSet.getLong("shop_id"),
                resultSet.getString("shop_external_id"),
                resultSet.getString("shop_name"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("current_price"),
                resultSet.getBigDecimal("original_price"),
                resultSet.getString("image_url"),
                resultSet.getString("product_url"),
                resultSet.getBigDecimal("rating"),
                resultSet.getLong("review_count"),
                instant(resultSet, "listed_at"),
                instant(resultSet, "collected_at"),
                latestStatDate,
                ProductStatus.valueOf(resultSet.getString("status"))
        );
    }

    private java.time.Instant instant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
