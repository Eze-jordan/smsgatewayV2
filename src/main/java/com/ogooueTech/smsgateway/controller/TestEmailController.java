package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEmailController {

    private final EmailService emailService;

    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/send-test-email")
    public String sendTest() {
        emailService.sendTestEmail("etsgood13@gmail.com");
        return "Email envoyé !";
    }
}
