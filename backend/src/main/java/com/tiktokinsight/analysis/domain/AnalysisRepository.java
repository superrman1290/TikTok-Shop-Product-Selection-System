package com.tiktokinsight.analysis.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalysisRepository {
    Optional<AnalysisProduct> findProduct(long productId);

    List<AnalysisDailyStat> findStats(long productId, LocalDate endDate);

    Optional<CostProfile> findMarketCost(String market);

    Optional<CostProfile> findUserCost(long userId, long productId);

    void saveUserCost(long userId, long productId, CostProfile profile, Instant now);

    boolean deleteUserCost(long userId, long productId);

    Optional<CategoryBenchmark> findBenchmark(String market, long categoryId, LocalDate statDate, String algorithmVersion);

    void saveBenchmark(String market, long categoryId, LocalDate statDate, String algorithmVersion, CategoryBenchmark benchmark, Instant now);

    List<Long> findActiveProductIds(String market, long categoryId);

    List<Long> findSevenDaySalesVolumes(String market, long categoryId, LocalDate statDate);

    Optional<AnalysisResult> findSnapshot(long productId, LocalDate analysisDate, String algorithmVersion);

    Optional<AnalysisResult> findLatestSnapshot(long productId, String algorithmVersion);

    void saveSnapshot(AnalysisResult result, Instant sourceDataUpdatedAt, Instant calculatedAt);

    void schedule(long productId, LocalDate analysisDate, String algorithmVersion, Instant now);

    List<AnalysisTarget> findRecalculationTargets(String algorithmVersion, int limit);

    List<AnalysisTarget> findAllActiveTargets();

    List<AnalysisJob> findRunnableJobs(Instant now, int limit);

    boolean claim(long jobId, Instant now);

    void complete(long jobId, Instant now);

    void retryOrFail(long jobId, int attempts, String message, Instant nextRetryAt, Instant now);

    Instant latestSourceUpdate(long productId);
}
