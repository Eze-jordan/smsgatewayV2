package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.AuditLog;
import com.ogooueTech.smsgateway.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service chargé de créer et de consulter les journaux d'audit.
 *
 * Les journaux d'audit permettent de conserver une trace des actions
 * importantes réalisées dans l'application :
 * - connexion ;
 * - modification d'un compte ;
 * - création ou suppression d'une ressource ;
 * - validation d'une opération sensible ;
 * - action d'administration.
 */
@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Injection du repository par constructeur.
     *
     * @param auditLogRepository repository des journaux d'audit
     */
    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Enregistre une nouvelle action dans le journal d'audit.
     *
     * Attention :
     * les mots de passe, tokens JWT, clés API et autres secrets
     * ne doivent jamais être placés dans la description.
     *
     * @param action type d'action réalisée
     * @param description description de l'action
     * @param userId identifiant de l'utilisateur
     * @param email adresse e-mail de l'utilisateur
     * @param role rôle de l'utilisateur
     * @param ip adresse IP à l'origine de la requête
     * @param userAgent navigateur ou client HTTP utilisé
     */
    public void logAction(
            String action,
            String description,
            String userId,
            String email,
            String role,
            String ip,
            String userAgent
    ) {
        AuditLog auditLog = new AuditLog();

        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setUserId(userId);
        auditLog.setUserEmail(email);
        auditLog.setRole(role);
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress(ip);
        auditLog.setUserAgent(userAgent);

        auditLogRepository.save(auditLog);
    }

    /**
     * Récupère tous les journaux d'audit.
     *
     * Les résultats sont triés du plus récent au plus ancien.
     *
     * @return liste complète des journaux d'audit
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAll() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Recherche les journaux d'audit associés à une adresse e-mail.
     *
     * @param email adresse e-mail de l'utilisateur recherché
     * @return liste des journaux de cet utilisateur
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getByUser(String email) {
        return auditLogRepository.findByUserEmail(email);
    }

    /**
     * Recherche les journaux enregistrés entre deux dates.
     *
     * @param start début de la période
     * @param end fin de la période
     * @return liste des journaux compris dans la période
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getByDate(
            LocalDateTime start,
            LocalDateTime end
    ) {
        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Les dates de début et de fin sont obligatoires"
            );
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "La date de début ne peut pas être postérieure à la date de fin"
            );
        }

        return auditLogRepository.findByTimestampBetween(start, end);
    }
}