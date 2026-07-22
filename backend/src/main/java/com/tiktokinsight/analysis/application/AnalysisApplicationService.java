package com.tiktokinsight.analysis.application;

import com.tiktokinsight.analysis.domain.AnalysisInput;
import com.tiktokinsight.analysis.domain.AnalysisJob;
import com.tiktokinsight.analysis.domain.AnalysisProduct;
import com.tiktokinsight.analysis.domain.AnalysisRepository;
import com.tiktokinsight.analysis.domain.AnalysisResult;
import com.tiktokinsight.analysis.domain.CategoryBenchmark;
import com.tiktokinsight.analysis.domain.SelectionV1Algorithm;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisApplicationService {

    private final AnalysisRepository repository;
    private final BenchmarkService benchmarkService;
    private final SelectionV1Algorithm algorithm;
    private final Clock clock;
    private final String activeVersion;

    public AnalysisApplicationService(
            AnalysisRepository repository,
            BenchmarkService benchmarkService,
            Clock clock,
            @Value("${app.analysis.active-version:selection-v1.0}") String activeVersion
    ) {
        this.repository = repository;
        this.benchmarkService = benchmarkService;
        this.algorithm = new SelectionV1Algorithm();
        this.clock = clock;
        this.activeVersion = activeVersion;
    }

    @Transactional
    public AnalysisResult latest(long productId, String requestedVersion) {
        String version = selectVersion(requestedVersion);
        AnalysisProduct product = requireProduct(productId);
        return repository.findLatestSnapshot(productId, version)
                .orElseGet(() -> {
                    if (product.latestStatDate() == null) {
                        throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ApiErrorCode.HISTORY_DATA_INSUFFICIENT);
                    }
                    return calculateAndSave(productId, product.latestStatDate(), version);
                });
    }

    @Transactional
    public AnalysisResult calculateAndSave(long productId, LocalDate analysisDate, String algorithmVersion) {
        AnalysisProduct product = requireProduct(productId);
        CategoryBenchmark benchmark = repository.findBenchmark(product.market(), product.categoryId(), analysisDate, algorithmVersion)
                .orElseGet(() -> benchmarkService.recalculate(product.market(), product.categoryId(), analysisDate, algorithmVersion));
        AnalysisResult result = algorithm.analyze(new AnalysisInput(
                product.id(), product.market(), product.categoryId(), product.currency(), product.listedDate(), analysisDate,
                algorithmVersion, repository.findStats(productId, analysisDate), repository.findMarketCost(product.market()).orElse(null), benchmark
        ));
        repository.saveSnapshot(result, repository.latestSourceUpdate(productId), clock.instant());
        return result;
    }

    @Transactional
    public void schedule(long productId, LocalDate analysisDate) {
        requireProduct(productId);
        repository.schedule(productId, analysisDate, activeVersion, clock.instant());
    }

    @Transactional
    public void processPendingJobs() {
        reconcileTargets();
        Instant now = clock.instant();
        for (AnalysisJob job : repository.findRunnableJobs(now, 50)) {
            if (!repository.claim(job.id(), now)) {
                continue;
            }
            try {
                calculateAndSave(job.productId(), job.analysisDate(), job.algorithmVersion());
                repository.complete(job.id(), clock.instant());
            } catch (RuntimeException exception) {
                repository.retryOrFail(job.id(), job.attemptCount() + 1, safeMessage(exception), retryAt(job.attemptCount() + 1), clock.instant());
            }
        }
    }

    private void reconcileTargets() {
        Set<String> benchmarkKeys = new HashSet<>();
        for (var target : repository.findRecalculationTargets(activeVersion, 50)) {
            String key = target.market() + ':' + target.categoryId() + ':' + target.analysisDate();
            if (benchmarkKeys.add(key)) {
                benchmarkService.recalculate(target.market(), target.categoryId(), target.analysisDate(), activeVersion);
            }
            repository.schedule(target.productId(), target.analysisDate(), activeVersion, clock.instant());
        }
    }

    public String activeVersion() {
        return activeVersion;
    }

    private AnalysisProduct requireProduct(long productId) {
        return repository.findProduct(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND));
    }

    private String selectVersion(String requestedVersion) {
        String version = requestedVersion == null || requestedVersion.isBlank() ? activeVersion : requestedVersion.trim();
        if (!SelectionV1Algorithm.VERSION.equals(version)) {
            throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.ALGORITHM_VERSION_NOT_FOUND);
        }
        return version;
    }

    private Instant retryAt(int attempt) {
        Duration delay = switch (attempt) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
        return clock.instant().plus(delay);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? "Analysis calculation failed" : message.substring(0, Math.min(message.length(), 500));
    }
}
