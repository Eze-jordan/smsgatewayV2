package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;

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
        List<String> Destinataires

) {
    @Override
    public String ref() {
        return ref;
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public String emetteur() {
        return emetteur;
    }

    @Override
    public String corps() {
        return corps;
    }

    @Override
    public SmsType type() {
        return type;
    }

    @Override
    public SmsStatus statut() {
        return statut;
    }

    @Override
    public LocalDateTime createdAt() {
        return createdAt;
    }

    @Override
    public List<String> Destinataires() {
        return Destinataires;
    }
}
