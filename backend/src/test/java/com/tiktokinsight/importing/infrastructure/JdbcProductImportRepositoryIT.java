package com.tiktokinsight.importing.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.tiktokinsight.datasource.ImportRow;
import com.tiktokinsight.datasource.ProductImportData;
import com.tiktokinsight.datasource.ProductStatImportData;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class JdbcProductImportRepositoryIT {

    private static final String DATABASE_NAME = "tiktok_insight_stage03_it";

    private static JdbcTemplate jdbc;
    private static JdbcProductImportRepository repository;
    private static DriverManagerDataSource adminDataSource;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        String host = System.getenv("TEST_MYSQL_HOST");
        Assumptions.assumeTrue(host != null && !host.isBlank(), "TEST_MYSQL_HOST is required");
        String password = System.getenv().getOrDefault("TEST_MYSQL_PASSWORD", "change_me");
        adminDataSource = new DriverManagerDataSource(
                "jdbc:mysql://" + host + ":3306/mysql?serverTimezone=UTC", "root", password
        );
        new JdbcTemplate(adminDataSource).execute("DROP DATABASE IF EXISTS " + DATABASE_NAME);
        new JdbcTemplate(adminDataSource).execute(
                "CREATE DATABASE " + DATABASE_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
        );
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:mysql://" + host + ":3306/" + DATABASE_NAME + "?serverTimezone=UTC",
                "root",
                password
        );
        jdbc = new JdbcTemplate(dataSource);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("../database/migrations/V1__create_auth_tables.sql"));
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("../database/migrations/V2__create_product_tables.sql"));
        }
        repository = new JdbcProductImportRepository(new NamedParameterJdbcTemplate(dataSource));
    }

    @AfterAll
    static void cleanUpDatabase() {
        if (adminDataSource != null) {
            new JdbcTemplate(adminDataSource).execute("DROP DATABASE IF EXISTS " + DATABASE_NAME);
        }
    }

    @Test
    void upsertsDimensionsProductsAndHistoricalStatsIdempotently() {
        ProductImportData product = new ProductImportData(
                "p-1", "TIKTOK_SHOP", "US", "Portable Blender", "home", "Home",
                "shop-1", "Healthy Store", "USD", new BigDecimal("29.99"),
                new BigDecimal("39.99"), null, null, new BigDecimal("4.60"), 100,
                Instant.parse("2026-06-15T08:30:00Z"), Instant.parse("2026-07-21T08:00:00Z")
        );

        assertThat(repository.upsertProducts(List.of(ImportRow.success(2, product))).successfulRows()).isEqualTo(1);
        ProductImportData updatedProduct = new ProductImportData(
                product.externalProductId(), product.platform(), product.market(), "Updated Blender",
                product.categoryExternalId(), product.categoryName(), product.shopExternalId(), product.shopName(),
                product.currency(), new BigDecimal("31.50"), product.originalPrice(), product.imageUrl(),
                product.productUrl(), product.rating(), product.reviewCount(), product.listedAt(), product.collectedAt()
        );
        assertThat(repository.upsertProducts(List.of(ImportRow.success(2, updatedProduct))).successfulRows()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shop", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT current_price FROM product WHERE external_product_id='p-1'", BigDecimal.class))
                .isEqualByComparingTo("31.50");

        ProductStatImportData newest = stat(LocalDate.parse("2026-07-20"), "25.00", 200);
        ProductStatImportData older = stat(LocalDate.parse("2026-07-19"), "20.00", 150);
        repository.upsertProductStats(List.of(ImportRow.success(2, newest)));
        repository.upsertProductStats(List.of(ImportRow.success(3, older)));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_daily_stat", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT current_price FROM product WHERE external_product_id='p-1'", BigDecimal.class))
                .isEqualByComparingTo("25.00");
        assertThat(jdbc.queryForObject("SELECT latest_stat_date FROM product WHERE external_product_id='p-1'", LocalDate.class))
                .isEqualTo(LocalDate.parse("2026-07-20"));

        repository.upsertProductStats(List.of(ImportRow.success(2, stat(
                LocalDate.parse("2026-07-20"), "24.50", 220
        ))));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_daily_stat", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT current_price FROM product WHERE external_product_id='p-1'", BigDecimal.class))
                .isEqualByComparingTo("24.50");
    }

    @Test
    void recordsMissingProductAsARowError() {
        ProductStatImportData missing = new ProductStatImportData(
                "missing", "TIKTOK_SHOP", "US", LocalDate.parse("2026-07-20"),
                new BigDecimal("10.00"), 1, new BigDecimal("10.00"), 1,
                1, 1, 1, 1, null, null, null, 1, new BigDecimal("4.00")
        );
        var result = repository.upsertProductStats(List.of(ImportRow.success(9, missing)));
        assertThat(result.successfulRows()).isZero();
        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.rowNumber()).isEqualTo(9);
            assertThat(error.errorCode()).isEqualTo("PRODUCT_NOT_FOUND");
        });
    }

    private ProductStatImportData stat(LocalDate date, String price, long totalSales) {
        return new ProductStatImportData(
                "p-1", "TIKTOK_SHOP", "US", date, new BigDecimal(price), 10,
                new BigDecimal("250.00"), totalSales, 5, 4, 3, 20,
                new BigDecimal("50.0000"), new BigDecimal("60.0000"), new BigDecimal("2.0000"),
                120, new BigDecimal("4.60")
        );
    }
}
