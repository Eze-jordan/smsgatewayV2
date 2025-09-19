package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ChangePasswordRequestClient;
import com.ogooueTech.smsgateway.dtos.ClientDTO;
import com.ogooueTech.smsgateway.dtos.SoldeNetResponse;
import com.ogooueTech.smsgateway.service.ClientService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/clients")
@Tag(name = "Clients", description = "Endpoints for managing client accounts, passwords, API keys, and balances")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Creation by an admin or manager: generates an API key and sends it by email to the client
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a client (by manager/admin)", tags = "Clients")
    public ResponseEntity<ClientDTO> createByManager(@Valid @RequestBody ClientDTO body) {
        ClientDTO saved = clientService.createByManager(body);
        return ResponseEntity
                .created(URI.create("/api/V1/clients/" + saved.getIdclients()))
                .body(saved);
    }

    /**
     * Endpoint to change a client's password
     * @param clientId client ID
     * @param request payload containing oldPassword and newPassword
     */
    @PatchMapping("/{clientId}/password")
    @Operation(summary = "Change client password", tags = "Clients")
    public ResponseEntity<?> changePassword(
            @PathVariable String clientId,
            @Valid @RequestBody ChangePasswordRequestClient request) {

        try {
            clientService.changePassword(clientId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all clients", tags = "Clients")
    public ResponseEntity<List<ClientDTO>> findAll() {
        return ResponseEntity.ok(clientService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get client by ID", tags = "Clients")
    public ResponseEntity<ClientDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(clientService.findById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update client (partial)", tags = "Clients")
    public ResponseEntity<ClientDTO> patch(@PathVariable String id, @RequestBody ClientDTO body) {
        return ResponseEntity.ok(clientService.patch(id, body));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete client", tags = "Clients")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/apikey")
    @Operation(summary = "Get API key for a client", tags = "Clients")
    public ResponseEntity<?> getApiKey(@PathVariable String id) {
        try {
            String apiKey = clientService.getApiKeyByClientId(id);
            return ResponseEntity.ok(Map.of("clientId", id, "apiKey", apiKey));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Server error", "details", ex.getMessage()));
        }
    }

    /**
     * Get the net balance for a client by technical ID
     */
    @GetMapping("/{id}/solde-net")
    @Operation(summary = "Get client's net balance (solde net)", tags = "Clients")
    public ResponseEntity<SoldeNetResponse> getSoldeNetById(@PathVariable String id) {
        return ResponseEntity.ok(clientService.getSoldeNetDtoByClientId(id));
    }
    // ✅ Suspendre un client
    @PostMapping("/{clientId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspendre un client", description = "Bloque toutes les actions de ce client")
    public ResponseEntity<?> suspendClient(@PathVariable String clientId) {
        clientService.suspendClient(clientId);
        return ResponseEntity.ok(Map.of(
                "message", "Client suspendu avec succès",
                "clientId", clientId
        ));
    }

    // ✅ Réactiver un client
    @PostMapping("/{clientId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réactiver un client", description = "Permet à un client suspendu de redevenir actif")
    public ResponseEntity<?> reactivateClient(@PathVariable String clientId) {
        clientService.reactivateClient(clientId);
        return ResponseEntity.ok(Map.of(
                "message", "Client réactivé avec succès",
                "clientId", clientId
        ));
    }
}
