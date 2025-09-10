package com.ogooueTech.smsgateway.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreditCreateRequest(
        @NotBlank String clientId,
        @Min(1) Integer quantity,
        @NotBlank String idempotencyKey
) {}