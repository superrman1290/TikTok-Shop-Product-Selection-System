package com.tiktokinsight.alert.application;

import java.time.Clock;
import java.time.LocalDate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertScheduler {
    private final AlertService service;
    private final Clock clock;
    public AlertScheduler(AlertService service, Clock clock) { this.service = service; this.clock = clock; }
    @Scheduled(cron = "${app.alert.daily-cron:0 0 2 * * *}")
    @SchedulerLock(name = "daily-alert-evaluation", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1S")
    public void evaluateDaily() { service.evaluate(LocalDate.now(clock)); }
}
