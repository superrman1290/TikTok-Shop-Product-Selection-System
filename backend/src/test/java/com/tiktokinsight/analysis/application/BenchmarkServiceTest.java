package com.tiktokinsight.analysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tiktokinsight.analysis.domain.AnalysisRepository;
import com.tiktokinsight.analysis.domain.CategoryBenchmark;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BenchmarkServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 30);

    @Test
    void excludesP75WhenThereAreFewerThanFiftyEligibleProducts() {
        AnalysisRepository repository = mock(AnalysisRepository.class);
        when(repository.findSevenDaySalesVolumes("US", 1, DATE)).thenReturn(volumes(49));
        BenchmarkService service = new BenchmarkService(repository, clock());

        CategoryBenchmark benchmark = service.recalculate("US", 1, DATE, "selection-v1.0");

        assertThat(benchmark.sampleCount()).isEqualTo(49);
        assertThat(benchmark.salesVolume7dP75()).isNull();
        assertThat(benchmark.explosiveSalesThreshold()).isEqualTo(100);
    }

    @Test
    void usesNearestRankP75ForFiftyEligibleProducts() {
        AnalysisRepository repository = mock(AnalysisRepository.class);
        when(repository.findSevenDaySalesVolumes("US", 1, DATE)).thenReturn(volumes(50));
        BenchmarkService service = new BenchmarkService(repository, clock());

        CategoryBenchmark benchmark = service.recalculate("US", 1, DATE, "selection-v1.0");

        assertThat(benchmark.salesVolume7dP75()).isEqualTo(38L);
        assertThat(benchmark.explosiveSalesThreshold()).isEqualTo(100);
        verify(repository).saveBenchmark(eq("US"), eq(1L), eq(DATE), eq("selection-v1.0"), eq(benchmark), any());
    }

    private List<Long> volumes(int size) {
        List<Long> values = new ArrayList<>();
        for (long index = 1; index <= size; index++) values.add(index);
        return values;
    }

    private Clock clock() {
        return Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC);
    }
}
