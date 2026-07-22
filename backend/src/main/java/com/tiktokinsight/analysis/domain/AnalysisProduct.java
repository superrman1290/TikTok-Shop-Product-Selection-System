package com.tiktokinsight.analysis.domain;

import java.time.LocalDate;

public record AnalysisProduct(
        long id,
        String market,
        long categoryId,
        String currency,
        String title,
        LocalDate listedDate,
        LocalDate latestStatDate
) {
}
