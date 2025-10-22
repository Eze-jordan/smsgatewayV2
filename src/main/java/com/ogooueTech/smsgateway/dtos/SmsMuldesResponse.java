package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SmsMuldesResponse(
        String ref,
        String clientId,
        String emetteur,
        String corps,
        SmsType type,
        SmsStatus statut,
        LocalDateTime createdAt,
        List<String> destinataires,
        LocalDate dateDebutEnvoi,
        LocalDate dateFinEnvoi,
        Integer nbParJour,
        Integer intervalleMinutes
) {
    // Constructeur alternatif sans les champs de planification (pour compatibilité)
    public SmsMuldesResponse(
            String ref,
            String clientId,
            String emetteur,
            String corps,
            SmsType type,
            SmsStatus statut,
            LocalDateTime createdAt,
            List<String> destinataires) {
        this(ref, clientId, emetteur, corps, type, statut, createdAt, destinataires, null, null, null, null);
    }
}