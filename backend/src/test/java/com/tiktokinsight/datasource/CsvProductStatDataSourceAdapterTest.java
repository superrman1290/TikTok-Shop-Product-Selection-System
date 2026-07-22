package com.tiktokinsight.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvProductStatDataSourceAdapterTest {

    @Test
    void parsesDailyStatisticsWithMoneyAsBigDecimal() {
        String csv = String.join(",", CsvProductStatDataSourceAdapter.HEADERS) + "\n"
                + "p-1,TIKTOK_SHOP,US,2026-07-20,29.99,368,11036.32,12842,57,39,6,42,63.50,71.20,4.80,1842,4.60\n";
        CsvProductStatDataSourceAdapter adapter = new CsvProductStatDataSourceAdapter();
        try (ProductStatDataBatch batch = adapter.fetchProductStats(new ProductStatFetchRequest(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 10
        ))) {
            List<ImportRow<ProductStatImportData>> rows = new ArrayList<>();
            batch.forEach(rows::add);
            assertThat(rows).hasSize(1);
            assertThat(rows.getFirst().data().salesAmount()).isEqualByComparingTo("11036.32");
            assertThat(rows.getFirst().data().statDate()).hasToString("2026-07-20");
        }
    }
}
