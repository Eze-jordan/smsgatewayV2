package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.UpdateTicketRequest;
import com.ogooueTech.smsgateway.model.Ticket;
import com.ogooueTech.smsgateway.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/V1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // Création ticket (par client)
    @PostMapping
    @Operation(summary = "Créer un ticket (client)")
    public ResponseEntity<Ticket> create(@RequestBody Ticket ticket) {
        return ResponseEntity.ok(ticketService.createTicket(ticket));
    }

    // Liste tous les tickets (ADMIN)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister tous les tickets (admin)")
    public ResponseEntity<List<Ticket>> findAll() {
        return ResponseEntity.ok(ticketService.findAll());
    }

    // Liste tickets d’un client
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Lister les tickets d’un client")
    public ResponseEntity<List<Ticket>> findByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(ticketService.findByClient(clientId));
    }

    // Mise à jour statut et réponse (ADMIN)
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour le statut d’un ticket (admin)")
    public ResponseEntity<Ticket> updateStatut(
            @PathVariable UUID id,
            @RequestBody UpdateTicketRequest body
    ) {
        return ResponseEntity.ok(
                ticketService.updateStatut(id, body.statut(), body.reponseAdmin())
        );
    }
}
