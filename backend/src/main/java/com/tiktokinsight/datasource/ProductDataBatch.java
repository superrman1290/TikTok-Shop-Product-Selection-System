package com.tiktokinsight.datasource;

public interface ProductDataBatch extends Iterable<ImportRow<ProductImportData>>, AutoCloseable {
    @Override
    void close();
}
