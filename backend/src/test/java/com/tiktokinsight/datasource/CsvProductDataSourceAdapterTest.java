package com.tiktokinsight.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvProductDataSourceAdapterTest {

    private final CsvProductDataSourceAdapter adapter = new CsvProductDataSourceAdapter();

    @Test
    void streamsValidUtf8ProductRows() {
        String csv = String.join(",", CsvProductDataSourceAdapter.HEADERS) + "\n"
                + "p-1,TIKTOK_SHOP,US,Portable Blender,home,Home,shop-1,Healthy Store,USD,29.99,39.99,"
                + "https://example.com/p.jpg,https://example.com/p/1,4.60,1842,2026-06-15T08:30:00Z,2026-07-21T08:00:00Z\n";

        try (ProductDataBatch batch = adapter.fetchProducts(request(csv))) {
            List<ImportRow<ProductImportData>> rows = stream(batch);
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().valid()).isTrue();
            assertThat(rows.getFirst().data().currentPrice()).isEqualByComparingTo("29.99");
            assertThat(rows.getFirst().data().market()).isEqualTo("US");
        }
    }

    @Test
    void recordsMarketCurrencyMismatchWithoutLeakingOtherFields() {
        String csv = String.join(",", CsvProductDataSourceAdapter.HEADERS) + "\n"
                + "p-1,TIKTOK_SHOP,US,Portable Blender,home,Home,shop-1,Healthy Store,GBP,29.99,39.99,,,,0,,2026-07-21T08:00:00Z\n";

        try (ProductDataBatch batch = adapter.fetchProducts(request(csv))) {
            ImportRow<ProductImportData> row = stream(batch).getFirst();
            assertThat(row.valid()).isFalse();
            assertThat(row.error().fieldName()).isEqualTo("currency");
            assertThat(row.error().errorCode()).isEqualTo("CURRENCY_MISMATCH");
            assertThat(row.error().rawValue()).isEqualTo("GBP");
        }
    }

    @Test
    void rejectsWrongHeadersAndRowLimit() {
        assertThatThrownBy(() -> adapter.fetchProducts(request("bad,header\n1,2\n")))
                .isInstanceOf(CsvFormatException.class);

        String csv = String.join(",", CsvProductDataSourceAdapter.HEADERS) + "\n"
                + validRow("p-1") + "\n" + validRow("p-2") + "\n";
        ProductFetchRequest request = new ProductFetchRequest(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 1
        );
        try (ProductDataBatch batch = adapter.fetchProducts(request)) {
            assertThatThrownBy(() -> stream(batch)).isInstanceOf(CsvFormatException.class);
        }
    }

    @Test
    void rejectsMalformedUtf8() {
        byte[] header = (String.join(",", CsvProductDataSourceAdapter.HEADERS) + "\n").getBytes(StandardCharsets.UTF_8);
        byte[] invalid = java.util.Arrays.copyOf(header, header.length + 2);
        invalid[header.length] = (byte) 0xC3;
        invalid[header.length + 1] = (byte) 0x28;
        assertThatThrownBy(() -> adapter.fetchProducts(new ProductFetchRequest(
                new ByteArrayInputStream(invalid), 10
        ))).isInstanceOf(CsvFormatException.class);
    }

    private ProductFetchRequest request(String csv) {
        return new ProductFetchRequest(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 10);
    }

    private List<ImportRow<ProductImportData>> stream(ProductDataBatch batch) {
        List<ImportRow<ProductImportData>> rows = new java.util.ArrayList<>();
        batch.forEach(rows::add);
        return rows;
    }

    private String validRow(String id) {
        return id + ",TIKTOK_SHOP,US,Portable Blender,home,Home,shop-1,Healthy Store,USD,29.99,39.99,,,,0,,2026-07-21T08:00:00Z";
    }
}
