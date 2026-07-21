package com.tiktokinsight.datasource;

import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvProductStatDataSourceAdapter implements ProductStatDataSourceAdapter {

    public static final List<String> HEADERS = List.of(
            "external_product_id", "platform", "market", "stat_date", "price", "sales_volume",
            "sales_amount", "total_sales_volume", "video_count", "creator_count", "shop_count",
            "similar_product_count", "top10_shop_sales_share", "top10_creator_sales_share",
            "negative_review_rate", "review_count", "rating"
    );

    @Override
    public DataSourceType supports() {
        return DataSourceType.CSV;
    }

    @Override
    public ProductStatDataBatch fetchProductStats(ProductStatFetchRequest request) {
        StreamingCsvBatch<ProductStatImportData> batch = new StreamingCsvBatch<>(
                request.content(), HEADERS, request.maxRows(), this::map
        );
        return new ProductStatDataBatch() {
            @Override
            public java.util.Iterator<ImportRow<ProductStatImportData>> iterator() {
                return batch.iterator();
            }

            @Override
            public void close() {
                batch.close();
            }
        };
    }

    private ProductStatImportData map(CSVRecord record) {
        String platform = CsvFieldParser.platform(record);
        SupportedMarket market = CsvFieldParser.market(record);
        return new ProductStatImportData(
                CsvFieldParser.required(record, "external_product_id", 160),
                platform,
                market.name(),
                CsvFieldParser.date(record, "stat_date"),
                CsvFieldParser.money(record, "price", true),
                CsvFieldParser.nonNegativeLong(record, "sales_volume"),
                CsvFieldParser.money(record, "sales_amount", true),
                CsvFieldParser.nonNegativeLong(record, "total_sales_volume"),
                CsvFieldParser.nonNegativeLong(record, "video_count"),
                CsvFieldParser.nonNegativeLong(record, "creator_count"),
                CsvFieldParser.nonNegativeLong(record, "shop_count"),
                CsvFieldParser.nonNegativeLong(record, "similar_product_count"),
                CsvFieldParser.percentage(record, "top10_shop_sales_share"),
                CsvFieldParser.percentage(record, "top10_creator_sales_share"),
                CsvFieldParser.percentage(record, "negative_review_rate"),
                CsvFieldParser.nonNegativeLong(record, "review_count"),
                CsvFieldParser.rating(record, "rating", false)
        );
    }
}
