package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ChangePasswordManager;
import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/V1/managers")
@PreAuthorize("hasAnyRole('ADMIN')")
@Tag(name = "Managers", description = "Endpoints for managing manager accounts (creation, activation, update, delete)")
public class ManagerController {

    @Autowired
    private ManagerService managerService;
    @Autowired
    private ManagerRepository managerRepository;


    @PostMapping("/create")
    @Operation(summary = "Create a manager account", tags = "Managers")
    public ResponseEntity<ManagerDTO> create(@Valid @RequestBody ManagerDTO body) {
        ManagerDTO manager = managerService.create(body);
        return ResponseEntity
                .created(URI.create("/api/V1/managers/" + manager.getIdManager()))
                .body(manager);
    }

    @PatchMapping("/{id}/change-password")
    @Operation(summary = "Modifier le mot de passe du manager", tags = "Managers")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordManager body) {

        managerService.changePassword(id, body.getOldPassword(), body.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Mot de passe modifié avec succès"
        ));
    }


    @GetMapping
    @Operation(summary = "Get all managers", tags = "Managers")
    public ResponseEntity<List<ManagerDTO>> findAll() {
        return ResponseEntity.ok(managerService.findAll());
    }



    @GetMapping("/{id}")
    @Operation(summary = "Get manager by ID", tags = "Managers")
    public ResponseEntity<ManagerDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(managerService.findById(id));
    }

    /** Partial update: send only the fields to modify */
    @PatchMapping("/{id}")
    @Operation(summary = "Update manager (partial)", tags = "Managers")
    public ResponseEntity<ManagerDTO> patch(@PathVariable String id, @RequestBody ManagerDTO body) {
        return ResponseEntity.ok(managerService.patch(id, body));
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a manager by ID", tags = "Managers")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
