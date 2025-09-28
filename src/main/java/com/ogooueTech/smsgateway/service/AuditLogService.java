package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.AuditLog;
import com.ogooueTech.smsgateway.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String action, String description,
                          String userId, String email, String role,
                          String ip, String userAgent) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setDescription(description);
        log.setUserId(userId);
        log.setUserEmail(email);
        log.setRole(role);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);

        auditLogRepository.save(log);
    }

    public List<AuditLog> getAll() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getByUser(String email) {
        return auditLogRepository.findByUserEmail(email);
    }

    public List<AuditLog> getByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    public List<AuditLog> getByDate(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }
}
