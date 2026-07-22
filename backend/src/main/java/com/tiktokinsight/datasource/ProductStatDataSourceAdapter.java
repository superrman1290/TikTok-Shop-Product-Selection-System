package com.tiktokinsight.datasource;

public interface ProductStatDataSourceAdapter {
    DataSourceType supports();

    ProductStatDataBatch fetchProductStats(ProductStatFetchRequest request);
}
