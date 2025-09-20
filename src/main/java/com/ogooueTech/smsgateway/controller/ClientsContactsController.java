package com.ogooueTech.smsgateway.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ogooueTech.smsgateway.model.ClientsContacts;
import com.ogooueTech.smsgateway.service.ClientsContactsService;
import com.ogooueTech.smsgateway.service.ClientsContactsService.ImportReportxls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(value = "/api/V1/contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Contacts", description = "Endpoints for managing client contacts and groups")
public class ClientsContactsController {

    private final ClientsContactsService service;

    public ClientsContactsController(ClientsContactsService service) {
        this.service = service;
    }

    /* ---------- Create (add a contact to a group) ---------- */
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a contact in a group", tags = "Contacts")
    public ResponseEntity<ClientsContacts> create(@Valid @RequestBody CreateContactRequest req) {
        ClientsContacts saved = service.addToGroup(req.groupId(), req.number(), req.name());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getIdClientsContacts())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    /* ---------- List ALL contacts ---------- */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "List all contacts", tags = "Contacts")
    public ResponseEntity<List<ClientsContacts>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    /* ---------- Get one (UUID) ---------- */
    @GetMapping("/{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Get contact by ID", tags = "Contacts")
    public ResponseEntity<ClientsContacts> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    /* ---------- List by group ---------- */
    @GetMapping("/group/{groupId}")
    @Operation(summary = "List contacts by group", tags = "Contacts")
    public ResponseEntity<List<ClientsContacts>> listByGroup(@PathVariable("groupId") @NotBlank String groupId) {
        return ResponseEntity.ok(service.listByGroup(groupId));
    }

    /* ---------- Patch (update number/name) ---------- */
    @PatchMapping(value = "/{id:[0-9a-fA-F\\-]{36}}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a contact (number or name)", tags = "Contacts")
    public ResponseEntity<ClientsContacts> patch(@PathVariable String id,
                                                 @Valid @RequestBody PatchContactRequest req) {
        return ResponseEntity.ok(service.patch(id, req.number(), req.name()));
    }

    /* ---------- Delete ---------- */
    @DeleteMapping("/{id:[0-9a-fA-F\\-]{36}}")
    @Operation(summary = "Delete a contact", tags = "Contacts")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Move (move a contact to another group) ---------- */
    @PostMapping(value = "/{contactId:[0-9a-fA-F\\-]{36}}/move", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Move a contact to another group", tags = "Contacts")
    public ResponseEntity<ClientsContacts> move(@PathVariable String contactId,
                                                @Valid @RequestBody MoveRequest req) {
        ClientsContacts moved = service.move(contactId, req.targetGroupId());
        return ResponseEntity.ok(moved);
    }

    /* ---------- Import CSV (multipart) ---------- */
    @PostMapping(value = "/import/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import contacts from CSV", tags = "Contacts")
    public ResponseEntity<ImportReportxls> importCsv(@PathVariable String groupId,
                                                     @RequestPart("file") MultipartFile file) throws IOException {
        ImportReportxls report = service.importCsvToGroup(groupId, file.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(report);
    }

    /* ---------- Import XLSX (multipart) ---------- */
    @PostMapping(value = "/import-xlsx/{groupId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import contacts from Excel (XLSX)", tags = "Contacts")
    public ResponseEntity<ClientsContactsService.ImportReport> importXlsx(@PathVariable String groupId,
                                                                          @RequestPart("file") MultipartFile file) throws IOException {
        ClientsContactsService.ImportReport report = service.importXlsxToGroup(groupId, file.getInputStream());
        return ResponseEntity.ok(report);
    }

    /* ---------- Search contacts ---------- */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Search contacts by name or number", tags = "Contacts")
    public ResponseEntity<List<ClientsContacts>> search(
            @RequestParam("q") String query
    ) {
        return ResponseEntity.ok(service.search(query));
    }

    @GetMapping("/client/{clientId}/search")
    @Operation(summary = "Search contacts of a client by name or number", tags = "Contacts")
    public ResponseEntity<List<ClientsContacts>> searchByClient(
            @PathVariable String clientId,
            @RequestParam("q") String query
    ) {
        return ResponseEntity.ok(service.searchByClient(clientId, query));
    }

    /* ---------- List contacts by client ---------- */
    @GetMapping("/client/{clientId}")
    @Operation(summary = "List contacts by client", tags = "Contacts")
    public ResponseEntity<List<ClientsContacts>> listByClient(@PathVariable("clientId") String clientId) {
        return ResponseEntity.ok(service.listByClient(clientId));
    }



    /* ===================== DTOs ===================== */

    public record CreateContactRequest(
            @NotBlank
            @JsonAlias({"group_id", "id_clients_groups", "idGroup", "group"})
            String groupId,
            @NotBlank String number,
            String name
    ) {}

    public record PatchContactRequest(String number, String name) {}

    public record MoveRequest(
            @NotBlank
            @JsonAlias({"target_group_id", "targetGroup", "groupId"})
            String targetGroupId
    ) {}


}
