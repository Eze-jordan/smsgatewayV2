package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.model.CreditRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreditRequestDto(
        UUID id,
        String clientId,
        Integer quantity,
        CreditStatus status,
        String makerEmail,
        String checkerEmail,
        String idempotencyKey,
        String rejectReason,
        LocalDateTime createdAt,
        LocalDateTime validatedAt,

        // ✅ nouveaux champs
        BigDecimal pricePerSmsTtc,
        BigDecimal estimatedAmountTtc
) {
    /** Mapping standard (sans prix) */
    public static CreditRequestDto from(CreditRequest cr) {
        return new CreditRequestDto(
                cr.getId(),
                cr.getClient().getIdclients(),
                cr.getQuantity(),
                cr.getStatus(),
                cr.getMakerEmail(),
                cr.getCheckerEmail(),
                cr.getIdempotencyKey(),
                cr.getRejectReason(),
                cr.getCreatedAt(),
                cr.getValidatedAt(),
                null,
                null
        );
    }

    /** Mapping enrichi (avec prix + montant estimé) */
    public static CreditRequestDto from(CreditRequest cr, BigDecimal pricePerSmsTtc, BigDecimal estimatedAmountTtc) {
        return new CreditRequestDto(
                cr.getId(),
                cr.getClient().getIdclients(),
                cr.getQuantity(),
                cr.getStatus(),
                cr.getMakerEmail(),
                cr.getCheckerEmail(),
                cr.getIdempotencyKey(),
                cr.getRejectReason(),
                cr.getCreatedAt(),
                cr.getValidatedAt(),
                pricePerSmsTtc,
                estimatedAmountTtc
        );
    }
}
