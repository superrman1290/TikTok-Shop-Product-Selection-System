package com.tiktokinsight.analysis.application;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalysisCompensationScheduler {
    private final AnalysisApplicationService service;

    public AnalysisCompensationScheduler(AnalysisApplicationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.analysis.daily-cron:0 30 1 * * *}")
    @SchedulerLock(name = "daily-analysis-compensation", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1S")
    public void compensateMissingSnapshots() {
        service.compensateMissingSnapshots();
    }
}
