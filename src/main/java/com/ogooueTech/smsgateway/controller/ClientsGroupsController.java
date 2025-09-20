package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.model.ClientsGroups;
import com.ogooueTech.smsgateway.service.ClientsGroupsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(value = "/api/V1/clientsGroups", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Groups", description = "Endpoints for managing client contact groups")
public class ClientsGroupsController {

    private final ClientsGroupsService service;

    public ClientsGroupsController(ClientsGroupsService service) {
        this.service = service;
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new group", tags = "Groups")
    public ResponseEntity<ClientsGroups> create(@Valid @RequestBody CreateGroupRequest req) {
        ClientsGroups saved = service.create(req.clientId(), req.nom(), req.description());
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getIdClientsGroups())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping("/all")
    @Operation(summary = "List all groups (for all clients)", tags = "Groups")
    public ResponseEntity<List<ClientsGroups>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping
    @Operation(summary = "List groups by client", tags = "Groups")
    public ResponseEntity<List<ClientsGroups>> listByClient(@RequestParam("clientId") @NotBlank String clientId) {
        return ResponseEntity.ok(service.listByClient(clientId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID", tags = "Groups")
    public ResponseEntity<ClientsGroups> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a group (partial)", tags = "Groups")
    public ResponseEntity<ClientsGroups> patch(@PathVariable String id, @Valid @RequestBody PatchGroupRequest req) {
        return ResponseEntity.ok(service.patch(id, req.nom(), req.description()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a group", tags = "Groups")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/client/{clientId}/search")
    @Operation(summary = "Search groups by keyword for a client", tags = "Groups")
    public ResponseEntity<List<ClientsGroups>> searchGroups(
            @PathVariable String clientId,
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(service.searchByClient(clientId, keyword));
    }

    // ---------- Payloads (records) ----------
    public record CreateGroupRequest(
            @NotBlank String clientId,
            @NotBlank String nom,
            String description
    ) {}

    public record PatchGroupRequest(
            String nom,
            String description
    ) {}
}
