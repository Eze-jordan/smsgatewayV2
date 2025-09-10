package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import com.ogooueTech.smsgateway.model.SmsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;


public interface SmsMessageRepository extends JpaRepository<SmsMessage, String> {
    List<SmsMessage> findByTypeAndStatut(SmsType type, SmsStatus statut);
    List<SmsMessage> findByStatutIn(Collection<SmsStatus> statuts);
    // Récupérer tous les SMS envoyés à un numéro précis
    List<SmsMessage> findByDestinataire(String destinataire);

    // Récupérer tous les SMS envoyés par un client (si tu veux filtrer par clientId)
    List<SmsMessage> findByClientId(String clientId);

    // Optionnel : récupérer par destinataire + statut (exemple : déjà envoyé)
    List<SmsMessage> findByDestinataireAndStatut(String destinataire, com.ogooueTech.smsgateway.enums.SmsStatus statut);


    // Base : tous les UNIDES d’un client (page + tri via Pageable)
    Page<SmsMessage> findByClientIdAndType(String clientId, SmsType type, Pageable pageable);

    // Filtre statut
    Page<SmsMessage> findByClientIdAndTypeAndStatut(String clientId, SmsType type, SmsStatus statut, Pageable pageable);

    // Filtre par date de création (avec ou sans statut)
    @Query("""
      SELECT m FROM SmsMessage m
      WHERE m.clientId = :clientId
        AND m.type = :type
        AND m.createdAt BETWEEN :start AND :end
      """)
    Page<SmsMessage> findByClientIdAndTypeBetweenDates(
            @Param("clientId") String clientId,
            @Param("type") SmsType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("""
      SELECT m FROM SmsMessage m
      WHERE m.clientId = :clientId
        AND m.type = :type
        AND m.statut = :statut
        AND m.createdAt BETWEEN :start AND :end
      """)
    Page<SmsMessage> findByClientIdAndTypeAndStatutBetweenDates(
            @Param("clientId") String clientId,
            @Param("type") SmsType type,
            @Param("statut") SmsStatus statut,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );


}
