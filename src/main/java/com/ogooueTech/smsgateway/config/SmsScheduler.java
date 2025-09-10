package com.ogooueTech.smsgateway.config;

import com.ogooueTech.smsgateway.service.SmsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SmsScheduler {
    private final SmsService smsService;

    public SmsScheduler(SmsService smsService) {
        this.smsService = smsService;
    }

    // toutes les minutes (ajuste selon besoin)
    @Scheduled(fixedDelay = 60_000)
    public void run() {
        smsService.tickPlanification();
    }
}
