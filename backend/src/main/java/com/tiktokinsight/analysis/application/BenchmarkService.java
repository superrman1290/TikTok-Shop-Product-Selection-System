package com.tiktokinsight.analysis.application;

import com.tiktokinsight.analysis.domain.AnalysisRepository;
import com.tiktokinsight.analysis.domain.CategoryBenchmark;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkService {

    private final AnalysisRepository repository;
    private final Clock clock;

    public BenchmarkService(AnalysisRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CategoryBenchmark recalculate(String market, long categoryId, LocalDate statDate, String algorithmVersion) {
        List<Long> salesVolumes = repository.findSevenDaySalesVolumes(market, categoryId, statDate);
        long sampleCount = salesVolumes.size();
        Long p75 = null;
        if (sampleCount >= 50) {
            int rank = (int) Math.ceil(sampleCount * 0.75d);
            p75 = salesVolumes.get(rank - 1);
        }
        CategoryBenchmark benchmark = new CategoryBenchmark(sampleCount, p75);
        repository.saveBenchmark(market, categoryId, statDate, algorithmVersion, benchmark, clock.instant());
        return benchmark;
    }
}
