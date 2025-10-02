package com.ogooueTech.smsgateway.config;
/*
import com.ogooueTech.smsgateway.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SmsScheduler {
    private static final Logger log = LoggerFactory.getLogger(SmsScheduler.class);
    private final SmsService smsService;

    public SmsScheduler(SmsService smsService) {
        this.smsService = smsService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        log.debug("⏰ Tick planification lancé...");
        smsService.tickPlanification();
    }
}
 */