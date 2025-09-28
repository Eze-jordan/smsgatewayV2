package com.ogooueTech.smsgateway.config;

import com.ogooueTech.smsgateway.service.AuditLogService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventsListener {

    private final AuditLogService auditLogService;

    public SecurityEventsListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof com.ogooueTech.smsgateway.securite.CustomUserDetails user) {
            auditLogService.logAction(
                    "LOGIN",
                    "Connexion réussie",
                    user.getId(),        // <-- récupère l’ID Manager/Client
                    user.getEmail(),
                    user.getRole(),
                    "N/A",               // tu pourras mettre la vraie IP plus tard
                    "N/A"
            );
        } else {
            // fallback si jamais ce n’est pas ton user
            auditLogService.logAction(
                    "LOGIN",
                    "Connexion réussie",
                    null,
                    event.getAuthentication().getName(),
                    event.getAuthentication().getAuthorities().toString(),
                    "N/A",
                    "N/A"
            );
        }
    }
}
