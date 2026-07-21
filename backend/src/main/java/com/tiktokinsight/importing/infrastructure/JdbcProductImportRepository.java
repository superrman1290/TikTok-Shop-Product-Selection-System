package com.tiktokinsight.importing.infrastructure;

import com.tiktokinsight.datasource.ImportRow;
import com.tiktokinsight.datasource.ImportRowError;
import com.tiktokinsight.datasource.ProductImportData;
import com.tiktokinsight.datasource.ProductStatImportData;
import com.tiktokinsight.importing.domain.BatchWriteResult;
import com.tiktokinsight.importing.domain.ProductImportRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcProductImportRepository implements ProductImportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProductImportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public BatchWriteResult upsertProducts(List<ImportRow<ProductImportData>> rows) {
        if (rows.isEmpty()) {
            return BatchWriteResult.success(0);
        }
        Map<DimensionKey, DimensionValue> categories = new LinkedHashMap<>();
        Map<DimensionKey, DimensionValue> shops = new LinkedHashMap<>();
        for (ImportRow<ProductImportData> row : rows) {
            ProductImportData data = row.data();
            categories.put(
                    new DimensionKey(data.platform(), data.market(), data.categoryExternalId()),
                    new DimensionValue(data.categoryName())
            );
            shops.put(
                    new DimensionKey(data.platform(), data.market(), data.shopExternalId()),
                    new DimensionValue(data.shopName())
            );
        }
        upsertDimensions("category", categories);
        upsertDimensions("shop", shops);
        Map<DimensionKey, Long> categoryIds = findDimensionIds("category", categories.keySet().stream().toList());
        Map<DimensionKey, Long> shopIds = findDimensionIds("shop", shops.keySet().stream().toList());

        SqlParameterSource[] parameters = rows.stream().map(row -> {
            ProductImportData data = row.data();
            return new MapSqlParameterSource()
                    .addValue("externalProductId", data.externalProductId())
                    .addValue("platform", data.platform())
                    .addValue("market", data.market())
                    .addValue("title", data.title())
                    .addValue("categoryId", categoryIds.get(new DimensionKey(
                            data.platform(), data.market(), data.categoryExternalId()
                    )))
                    .addValue("shopId", shopIds.get(new DimensionKey(
                            data.platform(), data.market(), data.shopExternalId()
                    )))
                    .addValue("currency", data.currency())
                    .addValue("currentPrice", data.currentPrice())
                    .addValue("originalPrice", data.originalPrice())
                    .addValue("imageUrl", data.imageUrl())
                    .addValue("productUrl", data.productUrl())
                    .addValue("rating", data.rating())
                    .addValue("reviewCount", data.reviewCount())
                    .addValue("listedAt", data.listedAt())
                    .addValue("collectedAt", data.collectedAt());
        }).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO product(
                    external_product_id, platform, market, title, category_id, shop_id, currency,
                    current_price, original_price, image_url, product_url, rating, review_count,
                    listed_at, collected_at, status, created_at, updated_at
                ) VALUES (
                    :externalProductId, :platform, :market, :title, :categoryId, :shopId, :currency,
                    :currentPrice, :originalPrice, :imageUrl, :productUrl, :rating, :reviewCount,
                    :listedAt, :collectedAt, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                ) AS incoming
                ON DUPLICATE KEY UPDATE
                    title = incoming.title,
                    category_id = incoming.category_id,
                    shop_id = incoming.shop_id,
                    currency = incoming.currency,
                    current_price = incoming.current_price,
                    original_price = incoming.original_price,
                    image_url = incoming.image_url,
                    product_url = incoming.product_url,
                    rating = incoming.rating,
                    review_count = incoming.review_count,
                    listed_at = incoming.listed_at,
                    collected_at = incoming.collected_at,
                    updated_at = UTC_TIMESTAMP(6)
                """, parameters);
        return BatchWriteResult.success(rows.size());
    }

    @Override
    @Transactional
    public BatchWriteResult upsertProductStats(List<ImportRow<ProductStatImportData>> rows) {
        if (rows.isEmpty()) {
            return BatchWriteResult.success(0);
        }
        List<ProductKey> keys = rows.stream()
                .map(row -> new ProductKey(
                        row.data().platform(), row.data().market(), row.data().externalProductId()
                ))
                .distinct()
                .toList();
        Map<ProductKey, Long> productIds = findProductIds(keys);
        List<ImportRow<ProductStatImportData>> available = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        for (ImportRow<ProductStatImportData> row : rows) {
            ProductStatImportData data = row.data();
            ProductKey key = new ProductKey(data.platform(), data.market(), data.externalProductId());
            if (!productIds.containsKey(key)) {
                errors.add(new ImportRowError(
                        row.rowNumber(), "external_product_id", data.externalProductId(),
                        "PRODUCT_NOT_FOUND", "商品唯一键不存在，请先导入商品基础数据"
                ));
            } else {
                available.add(row);
            }
        }
        if (!available.isEmpty()) {
            SqlParameterSource[] statParameters = available.stream().map(row -> {
                ProductStatImportData data = row.data();
                long productId = productIds.get(new ProductKey(
                        data.platform(), data.market(), data.externalProductId()
                ));
                return new MapSqlParameterSource()
                        .addValue("productId", productId)
                        .addValue("statDate", data.statDate())
                        .addValue("price", data.price())
                        .addValue("salesVolume", data.salesVolume())
                        .addValue("salesAmount", data.salesAmount())
                        .addValue("totalSalesVolume", data.totalSalesVolume())
                        .addValue("videoCount", data.videoCount())
                        .addValue("creatorCount", data.creatorCount())
                        .addValue("shopCount", data.shopCount())
                        .addValue("similarProductCount", data.similarProductCount())
                        .addValue("top10ShopSalesShare", data.top10ShopSalesShare())
                        .addValue("top10CreatorSalesShare", data.top10CreatorSalesShare())
                        .addValue("negativeReviewRate", data.negativeReviewRate())
                        .addValue("reviewCount", data.reviewCount())
                        .addValue("rating", data.rating());
            }).toArray(SqlParameterSource[]::new);
            jdbc.batchUpdate("""
                    INSERT INTO product_daily_stat(
                        product_id, stat_date, price, sales_volume, sales_amount, total_sales_volume,
                        video_count, creator_count, shop_count, similar_product_count,
                        top10_shop_sales_share, top10_creator_sales_share, negative_review_rate,
                        review_count, rating, created_at, updated_at
                    ) VALUES (
                        :productId, :statDate, :price, :salesVolume, :salesAmount, :totalSalesVolume,
                        :videoCount, :creatorCount, :shopCount, :similarProductCount,
                        :top10ShopSalesShare, :top10CreatorSalesShare, :negativeReviewRate,
                        :reviewCount, :rating, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
                    ) AS incoming
                    ON DUPLICATE KEY UPDATE
                        price = incoming.price,
                        sales_volume = incoming.sales_volume,
                        sales_amount = incoming.sales_amount,
                        total_sales_volume = incoming.total_sales_volume,
                        video_count = incoming.video_count,
                        creator_count = incoming.creator_count,
                        shop_count = incoming.shop_count,
                        similar_product_count = incoming.similar_product_count,
                        top10_shop_sales_share = incoming.top10_shop_sales_share,
                        top10_creator_sales_share = incoming.top10_creator_sales_share,
                        negative_review_rate = incoming.negative_review_rate,
                        review_count = incoming.review_count,
                        rating = incoming.rating,
                        updated_at = UTC_TIMESTAMP(6)
                    """, statParameters);
            jdbc.batchUpdate("""
                    UPDATE product
                       SET current_price = :price,
                           latest_stat_date = :statDate,
                           review_count = :reviewCount,
                           rating = :rating,
                           updated_at = UTC_TIMESTAMP(6)
                     WHERE id = :productId
                       AND (latest_stat_date IS NULL OR :statDate >= latest_stat_date)
                    """, statParameters);
        }
        return new BatchWriteResult(available.size(), errors);
    }

    private void upsertDimensions(String table, Map<DimensionKey, DimensionValue> values) {
        SqlParameterSource[] parameters = values.entrySet().stream().map(entry -> new MapSqlParameterSource()
                .addValue("platform", entry.getKey().platform())
                .addValue("market", entry.getKey().market())
                .addValue("externalId", entry.getKey().externalId())
                .addValue("name", entry.getValue().name())
        ).toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO %s(platform, market, external_id, name, created_at, updated_at)
                VALUES (:platform, :market, :externalId, :name, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)) AS incoming
                ON DUPLICATE KEY UPDATE name = incoming.name, updated_at = UTC_TIMESTAMP(6)
                """.formatted(table), parameters);
    }

    private Map<DimensionKey, Long> findDimensionIds(String table, List<DimensionKey> keys) {
        Map<DimensionKey, Long> result = new LinkedHashMap<>();
        Map<PlatformMarket, List<DimensionKey>> groups = keys.stream()
                .collect(Collectors.groupingBy(
                        key -> new PlatformMarket(key.platform(), key.market()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        groups.forEach((group, groupKeys) -> jdbc.query("""
                SELECT id, external_id FROM %s
                 WHERE platform = :platform AND market = :market AND external_id IN (:externalIds)
                """.formatted(table), new MapSqlParameterSource()
                .addValue("platform", group.platform())
                .addValue("market", group.market())
                .addValue("externalIds", groupKeys.stream().map(DimensionKey::externalId).toList()),
                (resultSet, rowNumber) -> Map.entry(
                        new DimensionKey(group.platform(), group.market(), resultSet.getString("external_id")),
                        resultSet.getLong("id")
                )
        ).forEach(entry -> result.put(entry.getKey(), entry.getValue())));
        return result;
    }

    private Map<ProductKey, Long> findProductIds(List<ProductKey> keys) {
        Map<ProductKey, Long> result = new LinkedHashMap<>();
        Map<PlatformMarket, List<ProductKey>> groups = keys.stream()
                .collect(Collectors.groupingBy(
                        key -> new PlatformMarket(key.platform(), key.market()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        groups.forEach((group, groupKeys) -> jdbc.query("""
                SELECT id, external_product_id FROM product
                 WHERE platform = :platform AND market = :market AND external_product_id IN (:externalIds)
                """, new MapSqlParameterSource()
                .addValue("platform", group.platform())
                .addValue("market", group.market())
                .addValue("externalIds", groupKeys.stream().map(ProductKey::externalProductId).toList()),
                (resultSet, rowNumber) -> Map.entry(
                        new ProductKey(group.platform(), group.market(), resultSet.getString("external_product_id")),
                        resultSet.getLong("id")
                )
        ).forEach(entry -> result.put(entry.getKey(), entry.getValue())));
        return result;
    }

    private record DimensionKey(String platform, String market, String externalId) {
    }

    private record DimensionValue(String name) {
    }

    private record ProductKey(String platform, String market, String externalProductId) {
    }

    private record PlatformMarket(String platform, String market) {
    }
}
