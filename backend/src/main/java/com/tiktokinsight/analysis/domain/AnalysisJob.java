package com.tiktokinsight.analysis.domain;

import java.time.Instant;
import java.time.LocalDate;

public record AnalysisJob(
        long id,
        long productId,
        LocalDate analysisDate,
        String algorithmVersion,
        Status status,
        int attemptCount,
        Instant nextRetryAt
) {
    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }
}
