package com.tiktokinsight.analysis.domain;

import java.time.LocalDate;

public record AnalysisTarget(long productId, String market, long categoryId, LocalDate analysisDate) {
}
