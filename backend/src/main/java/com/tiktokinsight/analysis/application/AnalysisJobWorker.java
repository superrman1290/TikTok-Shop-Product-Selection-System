package com.tiktokinsight.analysis.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
public class AnalysisJobWorker {

    private final AnalysisApplicationService service;

    public AnalysisJobWorker(AnalysisApplicationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.analysis.worker-delay-ms:10000}")
    @SchedulerLock(name = "analysis-job-worker", lockAtMostFor = "PT9M", lockAtLeastFor = "PT1S")
    public void processPendingJobs() {
        service.processPendingJobs();
    }
}
