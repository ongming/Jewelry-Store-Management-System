package com.example.Jewelry.scheduler;

import com.example.Jewelry.service.WeeklyCustomerDigestPublisherService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyCustomerDigestScheduler {

    private final WeeklyCustomerDigestPublisherService weeklyCustomerDigestPublisherService;

    @Value("${weekly.digest.enabled:true}")
    private boolean weeklyDigestEnabled;

    public WeeklyCustomerDigestScheduler(WeeklyCustomerDigestPublisherService weeklyCustomerDigestPublisherService) {
        this.weeklyCustomerDigestPublisherService = weeklyCustomerDigestPublisherService;
    }

    @Scheduled(cron = "${weekly.digest.cron:0 0 9 ? * SUN}", zone = "${weekly.digest.zone:Asia/Ho_Chi_Minh}")
    public void publishWeeklyDigest() {
        if (!weeklyDigestEnabled) {
            return;
        }
        weeklyCustomerDigestPublisherService.publishDigestForPreviousWeek();
    }
}

