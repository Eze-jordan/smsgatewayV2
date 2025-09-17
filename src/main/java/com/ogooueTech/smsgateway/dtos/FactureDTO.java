package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.model.Facture;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FactureDTO(
        String id,
        String clientId,
        LocalDate dateDebut,
        LocalDate dateFin,
        int consommationSms,
        BigDecimal prixUnitaire,
        BigDecimal montant
) {
    public static FactureDTO from(Facture f) {
        return new FactureDTO(
                f.getId(),
                f.getClient().getIdclients(),
                f.getDateDebut(),
                f.getDateFin(),
                f.getConsommationSms(),
                f.getPrixUnitaire(),
                f.getMontant()
        );
    }
}

