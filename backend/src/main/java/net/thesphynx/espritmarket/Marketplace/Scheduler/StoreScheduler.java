package net.thesphynx.espritmarket.Marketplace.Scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StoreScheduler {
    private final SchedulerService schedulerService;

    public StoreScheduler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(cron = "${app.marketplace.scheduler.nightly-cron:0 0 0 * * *}")
    @Transactional
    public void runNightlyStatusUpdate() {
        SchedulerService.NightlyMaintenanceResult result = schedulerService.runNightlyMaintenance();
        schedulerService.logNightlySummary(result);
    }
}
