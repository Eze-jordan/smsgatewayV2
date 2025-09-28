package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserEmail(String userEmail);
    List<AuditLog> findByAction(String action);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Tri par date
    List<AuditLog> findAllByOrderByTimestampDesc();
}
