package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.*;
import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.service.CreditService;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/credits")
@Tag(name = "Credits", description = "Endpoints for managing credit requests (create, approve, reject, list)")
public class CreditController {

    private final CreditService service;

    public CreditController(CreditService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT_USER')")
    @Operation(summary = "Create a new credit request", tags = "Credits")
    public CreditRequestDto create(@RequestBody CreditCreateRequest body, Authentication auth) {
        // Optional: additional security
        // if current user is MAKER, verify they are creating for "their" clientId
        return service.create(
                body.clientId(),
                body.quantity(),
                body.idempotencyKey()
        );
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Approve a credit request", tags = "Credits")
    public CreditRequestDto approve(@PathVariable UUID id, Authentication auth) {
        return service.approve(id, auth != null ? auth.getName() : "checker@system");
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Reject a credit request", tags = "Credits")
    public CreditRequestDto reject(@PathVariable UUID id, @RequestBody CreditRejectRequest body, Authentication auth) {
        return service.reject(id, auth != null ? auth.getName() : "checker@system", body.reason());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MAKER','CHECKER','ADMIN')")
    @Operation(summary = "List credit requests with optional filters", tags = "Credits")
    public Page<CreditRequestDto> list(@RequestParam(required = false) String clientId,
                                       @RequestParam(required = false) CreditStatus status,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return service.list(clientId, status, pageable);
    }
}
