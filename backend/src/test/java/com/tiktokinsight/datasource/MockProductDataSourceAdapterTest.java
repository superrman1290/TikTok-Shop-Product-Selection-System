package com.tiktokinsight.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class MockProductDataSourceAdapterTest {

    @Test
    void returnsDeterministicSeedData() {
        MockProductDataSourceAdapter adapter = new MockProductDataSourceAdapter();
        try (ProductDataBatch batch = adapter.fetchProducts(new ProductFetchRequest(
                new ByteArrayInputStream(new byte[0]), 10
        ))) {
            ImportRow<ProductImportData> row = batch.iterator().next();
            assertThat(adapter.supports()).isEqualTo(DataSourceType.MOCK);
            assertThat(row.data().externalProductId()).isEqualTo("mock-20260721-1");
        }
    }
}
