package com.ogooueTech.smsgateway.dtos;

import java.time.LocalDateTime;

public record EmetteurResponse(
        String id,
        String nom,
        LocalDateTime createdAt
) {}
