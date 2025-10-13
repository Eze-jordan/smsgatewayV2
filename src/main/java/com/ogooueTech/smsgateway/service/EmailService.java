package com.ogooueTech.smsgateway.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    public void sendTestEmail(String toEmail) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Test Email");
        msg.setText("Bonjour, ceci est un test.");
        msg.setFrom("notify@eservices-gabon.com");
        javaMailSender.send(msg);
    }
}
