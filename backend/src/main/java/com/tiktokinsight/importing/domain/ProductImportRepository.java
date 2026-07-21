package com.tiktokinsight.importing.domain;

import com.tiktokinsight.datasource.ImportRow;
import com.tiktokinsight.datasource.ProductImportData;
import com.tiktokinsight.datasource.ProductStatImportData;
import java.util.List;

public interface ProductImportRepository {
    BatchWriteResult upsertProducts(List<ImportRow<ProductImportData>> rows);

    BatchWriteResult upsertProductStats(List<ImportRow<ProductStatImportData>> rows);
}
