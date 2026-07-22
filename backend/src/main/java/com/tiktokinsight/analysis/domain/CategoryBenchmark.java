package com.tiktokinsight.analysis.domain;

public record CategoryBenchmark(long sampleCount, Long salesVolume7dP75) {
    public long explosiveSalesThreshold() {
        return sampleCount < 50 || salesVolume7dP75 == null ? 100 : Math.max(100, salesVolume7dP75);
    }
}
