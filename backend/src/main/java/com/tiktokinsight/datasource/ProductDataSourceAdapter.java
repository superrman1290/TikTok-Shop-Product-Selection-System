package com.tiktokinsight.datasource;

public interface ProductDataSourceAdapter {
    DataSourceType supports();

    ProductDataBatch fetchProducts(ProductFetchRequest request);
}
