package com.tiktokinsight.importing.application;

import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import com.tiktokinsight.datasource.CsvFormatException;
import com.tiktokinsight.datasource.DataSourceType;
import com.tiktokinsight.datasource.ImportRow;
import com.tiktokinsight.datasource.ImportRowError;
import com.tiktokinsight.datasource.ProductDataSourceAdapter;
import com.tiktokinsight.datasource.ProductFetchRequest;
import com.tiktokinsight.datasource.ProductImportData;
import com.tiktokinsight.datasource.ProductStatDataSourceAdapter;
import com.tiktokinsight.datasource.ProductStatFetchRequest;
import com.tiktokinsight.datasource.ProductStatImportData;
import com.tiktokinsight.importing.domain.BatchWriteResult;
import com.tiktokinsight.importing.domain.ImportJob;
import com.tiktokinsight.importing.domain.ImportJobRepository;
import com.tiktokinsight.importing.domain.ImportStatus;
import com.tiktokinsight.importing.domain.ImportType;
import com.tiktokinsight.importing.domain.ProductImportRepository;
import com.tiktokinsight.storage.ObjectStorage;
import com.tiktokinsight.storage.StorageUploadRequest;
import com.tiktokinsight.storage.StoredObject;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportApplicationService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_ROWS = 200_000;
    private static final int BATCH_SIZE = 1_000;
    private static final Set<String> CSV_MIME_TYPES = Set.of(
            "text/csv", "application/csv", "application/vnd.ms-excel"
    );

    private final ObjectStorage objectStorage;
    private final ProductImportRepository productRepository;
    private final ImportJobRepository jobRepository;
    private final Map<DataSourceType, ProductDataSourceAdapter> productAdapters;
    private final Map<DataSourceType, ProductStatDataSourceAdapter> statAdapters;

    public ImportApplicationService(
            ObjectStorage objectStorage,
            ProductImportRepository productRepository,
            ImportJobRepository jobRepository,
            List<ProductDataSourceAdapter> productAdapters,
            List<ProductStatDataSourceAdapter> statAdapters
    ) {
        this.objectStorage = objectStorage;
        this.productRepository = productRepository;
        this.jobRepository = jobRepository;
        this.productAdapters = adaptersByType(productAdapters);
        this.statAdapters = adaptersByType(statAdapters);
    }

    public ImportJob importProducts(MultipartFile file, long userId) {
        Upload upload = store(file);
        long jobId = jobRepository.create(
                ImportType.PRODUCT, DataSourceType.CSV, upload.fileName(), upload.storedObject().objectKey(), userId
        );
        Counts counts = new Counts();
        try (InputStream content = objectStorage.get(upload.storedObject().objectKey());
             var batch = productAdapter(DataSourceType.CSV).fetchProducts(new ProductFetchRequest(content, MAX_ROWS))) {
            List<ImportRow<ProductImportData>> pending = new ArrayList<>(BATCH_SIZE);
            List<ImportRowError> errors = new ArrayList<>(BATCH_SIZE);
            for (ImportRow<ProductImportData> row : batch) {
                counts.total++;
                if (row.valid()) {
                    pending.add(row);
                    if (pending.size() == BATCH_SIZE) {
                        writeProducts(jobId, pending, counts);
                    }
                } else {
                    errors.add(row.error());
                    counts.failed++;
                    if (errors.size() == BATCH_SIZE) {
                        saveErrors(jobId, errors);
                    }
                }
            }
            writeProducts(jobId, pending, counts);
            saveErrors(jobId, errors);
            finish(jobId, counts);
            return get(jobId);
        } catch (CsvFormatException exception) {
            fail(jobId, counts);
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CSV_FORMAT_ERROR);
        } catch (RuntimeException exception) {
            fail(jobId, counts);
            throw exception;
        } catch (Exception exception) {
            fail(jobId, counts);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.DATA_SOURCE_UNAVAILABLE);
        }
    }

    public ImportJob importProductStats(MultipartFile file, long userId) {
        Upload upload = store(file);
        long jobId = jobRepository.create(
                ImportType.PRODUCT_STAT, DataSourceType.CSV, upload.fileName(), upload.storedObject().objectKey(), userId
        );
        Counts counts = new Counts();
        try (InputStream content = objectStorage.get(upload.storedObject().objectKey());
             var batch = statAdapter(DataSourceType.CSV).fetchProductStats(new ProductStatFetchRequest(content, MAX_ROWS))) {
            List<ImportRow<ProductStatImportData>> pending = new ArrayList<>(BATCH_SIZE);
            List<ImportRowError> errors = new ArrayList<>(BATCH_SIZE);
            for (ImportRow<ProductStatImportData> row : batch) {
                counts.total++;
                if (row.valid()) {
                    pending.add(row);
                    if (pending.size() == BATCH_SIZE) {
                        writeStats(jobId, pending, counts);
                    }
                } else {
                    errors.add(row.error());
                    counts.failed++;
                    if (errors.size() == BATCH_SIZE) {
                        saveErrors(jobId, errors);
                    }
                }
            }
            writeStats(jobId, pending, counts);
            saveErrors(jobId, errors);
            finish(jobId, counts);
            return get(jobId);
        } catch (CsvFormatException exception) {
            fail(jobId, counts);
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CSV_FORMAT_ERROR);
        } catch (RuntimeException exception) {
            fail(jobId, counts);
            throw exception;
        } catch (Exception exception) {
            fail(jobId, counts);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.DATA_SOURCE_UNAVAILABLE);
        }
    }

    public PageResponse<ImportJob> list(int page, int pageSize) {
        validatePage(page, pageSize);
        return jobRepository.findAll(page, pageSize);
    }

    public ImportJob get(long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.IMPORT_JOB_NOT_FOUND));
    }

    public PageResponse<ImportRowError> errors(long jobId, int page, int pageSize) {
        validatePage(page, pageSize);
        get(jobId);
        return jobRepository.findErrors(jobId, page, pageSize);
    }

    private Upload store(MultipartFile file) {
        validateFile(file);
        String fileName = safeFileName(file.getOriginalFilename());
        try {
            StoredObject stored = objectStorage.put(new StorageUploadRequest(
                    fileName, file.getContentType(), file.getInputStream()
            ));
            if (stored.size() > MAX_FILE_SIZE) {
                objectStorage.delete(stored.objectKey());
                throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.FILE_TOO_LARGE);
            }
            return new Upload(fileName, stored);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.DATA_SOURCE_UNAVAILABLE);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CSV_FORMAT_ERROR);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.FILE_TOO_LARGE);
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CSV_FORMAT_ERROR);
        }
        String contentType = file.getContentType();
        if (contentType == null || !CSV_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.CSV_FORMAT_ERROR);
        }
    }

    private String safeFileName(String original) {
        try {
            String value = Path.of(original == null ? "upload.csv" : original).getFileName().toString();
            return value.length() <= 255 ? value : value.substring(value.length() - 255);
        } catch (InvalidPathException exception) {
            return "upload.csv";
        }
    }

    private void writeProducts(long jobId, List<ImportRow<ProductImportData>> pending, Counts counts) {
        if (pending.isEmpty()) {
            return;
        }
        BatchWriteResult result = productRepository.upsertProducts(List.copyOf(pending));
        counts.success += result.successfulRows();
        counts.failed += result.errors().size();
        jobRepository.saveErrors(jobId, result.errors());
        pending.clear();
    }

    private void writeStats(long jobId, List<ImportRow<ProductStatImportData>> pending, Counts counts) {
        if (pending.isEmpty()) {
            return;
        }
        BatchWriteResult result = productRepository.upsertProductStats(List.copyOf(pending));
        counts.success += result.successfulRows();
        counts.failed += result.errors().size();
        jobRepository.saveErrors(jobId, result.errors());
        pending.clear();
    }

    private void saveErrors(long jobId, List<ImportRowError> errors) {
        jobRepository.saveErrors(jobId, List.copyOf(errors));
        errors.clear();
    }

    private void finish(long jobId, Counts counts) {
        jobRepository.increment(jobId, counts.total, counts.success, counts.failed);
        counts.persisted = true;
        ImportStatus status = counts.failed == 0 ? ImportStatus.SUCCESS : ImportStatus.PARTIAL_SUCCESS;
        jobRepository.complete(jobId, status);
    }

    private void fail(long jobId, Counts counts) {
        if (!counts.persisted) {
            jobRepository.increment(jobId, counts.total, counts.success, counts.failed);
            counts.persisted = true;
        }
        jobRepository.fail(jobId);
    }

    private ProductDataSourceAdapter productAdapter(DataSourceType type) {
        ProductDataSourceAdapter adapter = productAdapters.get(type);
        if (adapter == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.DATA_SOURCE_UNAVAILABLE);
        }
        return adapter;
    }

    private ProductStatDataSourceAdapter statAdapter(DataSourceType type) {
        ProductStatDataSourceAdapter adapter = statAdapters.get(type);
        if (adapter == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.DATA_SOURCE_UNAVAILABLE);
        }
        return adapter;
    }

    private static <T> Map<DataSourceType, T> adaptersByType(List<T> adapters) {
        Map<DataSourceType, T> result = new EnumMap<>(DataSourceType.class);
        for (T adapter : adapters) {
            DataSourceType type;
            if (adapter instanceof ProductDataSourceAdapter productAdapter) {
                type = productAdapter.supports();
            } else if (adapter instanceof ProductStatDataSourceAdapter statAdapter) {
                type = statAdapter.supports();
            } else {
                throw new IllegalArgumentException("Unsupported adapter type");
            }
            if (result.put(type, adapter) != null) {
                throw new IllegalStateException("Duplicate adapter for " + type);
            }
        }
        return result;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
        }
    }

    private record Upload(String fileName, StoredObject storedObject) {
    }

    private static final class Counts {
        private long total;
        private long success;
        private long failed;
        private boolean persisted;
    }
}
