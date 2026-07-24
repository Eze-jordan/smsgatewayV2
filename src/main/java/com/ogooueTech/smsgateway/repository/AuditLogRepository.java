package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository chargé de gérer les opérations d'accès aux données
 * concernant les journaux d'audit.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - enregistrer un journal ;
 * - rechercher un journal par son identifiant ;
 * - récupérer tous les journaux ;
 * - modifier un journal ;
 * - supprimer un journal.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Recherche tous les journaux d'audit associés à l'adresse e-mail
     * d'un utilisateur.
     *
     * @param userEmail adresse e-mail de l'utilisateur recherché
     * @return liste des journaux correspondant à cet utilisateur
     */
    List<AuditLog> findByUserEmail(String userEmail);

    /**
     * Recherche tous les journaux d'audit correspondant à une action donnée.
     *
     * Exemples d'actions :
     * - LOGIN
     * - LOGOUT
     * - CREATE_USER
     * - UPDATE_USER
     * - DELETE_USER
     *
     * @param action nom ou type de l'action recherchée
     * @return liste des journaux correspondant à cette action
     */
    List<AuditLog> findByAction(String action);

    /**
     * Recherche tous les journaux d'audit enregistrés entre deux dates.
     *
     * @param start date et heure de début de la période
     * @param end date et heure de fin de la période
     * @return liste des journaux compris dans la période demandée
     */
    List<AuditLog> findByTimestampBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Récupère tous les journaux d'audit triés par date décroissante.
     *
     * Les événements les plus récents apparaissent donc en premier.
     *
     * @return liste complète des journaux triés du plus récent au plus ancien
     */
    List<AuditLog> findAllByOrderByTimestampDesc();
}