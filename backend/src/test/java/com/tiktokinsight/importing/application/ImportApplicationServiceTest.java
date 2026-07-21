package com.tiktokinsight.importing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.datasource.CsvProductDataSourceAdapter;
import com.tiktokinsight.datasource.CsvProductStatDataSourceAdapter;
import com.tiktokinsight.datasource.MockProductDataSourceAdapter;
import com.tiktokinsight.importing.domain.BatchWriteResult;
import com.tiktokinsight.importing.domain.ImportJob;
import com.tiktokinsight.importing.domain.ImportJobRepository;
import com.tiktokinsight.importing.domain.ImportStatus;
import com.tiktokinsight.importing.domain.ImportType;
import com.tiktokinsight.importing.domain.ProductImportRepository;
import com.tiktokinsight.storage.ObjectStorage;
import com.tiktokinsight.storage.StoredObject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImportApplicationServiceTest {

    @Test
    void importsInBatchesOfOneThousandAndCompletesJob() {
        ObjectStorage storage = mock(ObjectStorage.class);
        ProductImportRepository products = mock(ProductImportRepository.class);
        ImportJobRepository jobs = mock(ImportJobRepository.class);
        String csv = productCsv(1_001);
        when(storage.put(any())).thenReturn(new StoredObject("generated.csv", csv.length(), "text/csv"));
        when(storage.get("generated.csv")).thenReturn(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
        when(jobs.create(any(), any(), any(), any(), anyLong())).thenReturn(9L);
        when(products.upsertProducts(any())).thenAnswer(invocation ->
                BatchWriteResult.success(invocation.<List<?>>getArgument(0).size()));
        when(jobs.findById(9L)).thenReturn(Optional.of(job(9, 1_001, 1_001, 0, ImportStatus.SUCCESS)));
        ImportApplicationService service = new ImportApplicationService(
                storage,
                products,
                jobs,
                List.of(new CsvProductDataSourceAdapter(), new MockProductDataSourceAdapter()),
                List.of(new CsvProductStatDataSourceAdapter())
        );

        ImportJob result = service.importProducts(new MockMultipartFile(
                "file", "products.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)
        ), 1L);

        assertThat(result.status()).isEqualTo(ImportStatus.SUCCESS);
        verify(products, times(2)).upsertProducts(any());
        verify(jobs).increment(9L, 1_001, 1_001, 0);
        verify(jobs).complete(9L, ImportStatus.SUCCESS);
    }

    @Test
    void exposesOnlyBoundedImportErrorPages() {
        ImportJobRepository jobs = mock(ImportJobRepository.class);
        when(jobs.findById(5L)).thenReturn(Optional.of(job(5, 2, 1, 1, ImportStatus.PARTIAL_SUCCESS)));
        when(jobs.findErrors(5L, 1, 100)).thenReturn(new PageResponse<>(1, 100, 0, List.of()));
        ImportApplicationService service = new ImportApplicationService(
                mock(ObjectStorage.class), mock(ProductImportRepository.class), jobs,
                List.of(new CsvProductDataSourceAdapter(), new MockProductDataSourceAdapter()),
                List.of(new CsvProductStatDataSourceAdapter())
        );
        assertThat(service.errors(5L, 1, 100).pageSize()).isEqualTo(100);
    }

    private String productCsv(int rows) {
        StringBuilder csv = new StringBuilder(String.join(",", CsvProductDataSourceAdapter.HEADERS)).append('\n');
        for (int index = 1; index <= rows; index++) {
            csv.append("p-").append(index)
                    .append(",TIKTOK_SHOP,US,Product ").append(index)
                    .append(",home,Home,shop-1,Store,USD,29.99,39.99,,,,0,,2026-07-21T08:00:00Z\n");
        }
        return csv.toString();
    }

    private ImportJob job(long id, long total, long success, long failed, ImportStatus status) {
        return new ImportJob(
                id, ImportType.PRODUCT, com.tiktokinsight.datasource.DataSourceType.CSV, status,
                "products.csv", total, success, failed, 1, "admin",
                Instant.parse("2026-07-21T08:00:00Z"), Instant.parse("2026-07-21T08:01:00Z"),
                Instant.parse("2026-07-21T08:00:00Z")
        );
    }
}
