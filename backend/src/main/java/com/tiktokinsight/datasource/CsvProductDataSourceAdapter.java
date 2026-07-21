package com.tiktokinsight.datasource;

import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvProductDataSourceAdapter implements ProductDataSourceAdapter {

    public static final List<String> HEADERS = List.of(
            "external_product_id", "platform", "market", "title", "category_external_id",
            "category_name", "shop_external_id", "shop_name", "currency", "current_price",
            "original_price", "image_url", "product_url", "rating", "review_count", "listed_at",
            "collected_at"
    );

    @Override
    public DataSourceType supports() {
        return DataSourceType.CSV;
    }

    @Override
    public ProductDataBatch fetchProducts(ProductFetchRequest request) {
        StreamingCsvBatch<ProductImportData> batch = new StreamingCsvBatch<>(
                request.content(), HEADERS, request.maxRows(), this::map
        );
        return new ProductDataBatch() {
            @Override
            public java.util.Iterator<ImportRow<ProductImportData>> iterator() {
                return batch.iterator();
            }

            @Override
            public void close() {
                batch.close();
            }
        };
    }

    private ProductImportData map(CSVRecord record) {
        String platform = CsvFieldParser.platform(record);
        SupportedMarket market = CsvFieldParser.market(record);
        String currency = CsvFieldParser.currency(record, market);
        return new ProductImportData(
                CsvFieldParser.required(record, "external_product_id", 160),
                platform,
                market.name(),
                CsvFieldParser.required(record, "title", 500),
                CsvFieldParser.required(record, "category_external_id", 160),
                CsvFieldParser.required(record, "category_name", 200),
                CsvFieldParser.required(record, "shop_external_id", 160),
                CsvFieldParser.required(record, "shop_name", 200),
                currency,
                CsvFieldParser.money(record, "current_price", true),
                CsvFieldParser.money(record, "original_price", false),
                CsvFieldParser.url(record, "image_url"),
                CsvFieldParser.url(record, "product_url"),
                CsvFieldParser.rating(record, "rating", false),
                CsvFieldParser.nonNegativeLong(record, "review_count"),
                CsvFieldParser.instant(record, "listed_at", false),
                CsvFieldParser.instant(record, "collected_at", true)
        );
    }
}
