package com.tiktokinsight.datasource;

public interface ProductStatDataBatch extends Iterable<ImportRow<ProductStatImportData>>, AutoCloseable {
    @Override
    void close();
}
