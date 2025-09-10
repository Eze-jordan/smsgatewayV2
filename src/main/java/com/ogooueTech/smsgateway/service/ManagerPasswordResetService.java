package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.securite.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ManagerPasswordResetService {
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Value("${app.security.reset-exp-minutes:30}")
    private int resetExpiryMinutes;

    public ManagerPasswordResetService(ManagerRepository managerRepository,
                                       PasswordEncoder passwordEncoder,
                                       NotificationService notificationService,
                                       JwtService jwtService) {
        this.managerRepository = managerRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    /** Étape 1 : génération et envoi du token */
    public void forgotPassword(String email) {
        Manager manager = managerRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Aucun manager avec cet email"));
        String token = jwtService.generatePasswordResetToken(manager.getIdManager(), resetExpiryMinutes);
        notificationService.envoyerResetManager(manager, token);
    }

    /** Étape 2 : validation token + update password */
    public void resetPassword(String token, String newPassword) {
        jwtService.assertValidPasswordResetToken(token);
        String managerId = jwtService.extractSubjectFromPasswordResetToken(token);

        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        manager.setMotDePasseManager(passwordEncoder.encode(newPassword));
        managerRepository.save(manager);
    }
}
