package com.tiktokinsight.datasource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockProductDataSourceAdapter implements ProductDataSourceAdapter {

    @Override
    public DataSourceType supports() {
        return DataSourceType.MOCK;
    }

    @Override
    public ProductDataBatch fetchProducts(ProductFetchRequest request) {
        List<ImportRow<ProductImportData>> rows = List.of(ImportRow.success(1, new ProductImportData(
                "mock-20260721-1", "TIKTOK_SHOP", "US", "Mock Portable Blender",
                "mock-home", "Home Appliances", "mock-shop", "Mock Store", "USD",
                new BigDecimal("29.99"), new BigDecimal("39.99"), null, null,
                new BigDecimal("4.60"), 1842, Instant.parse("2026-06-15T08:30:00Z"),
                Instant.parse("2026-07-21T08:00:00Z")
        )));
        return new ProductDataBatch() {
            @Override
            public Iterator<ImportRow<ProductImportData>> iterator() {
                return rows.iterator();
            }

            @Override
            public void close() {
                // No external resource is held by the in-memory adapter.
            }
        };
    }
}
