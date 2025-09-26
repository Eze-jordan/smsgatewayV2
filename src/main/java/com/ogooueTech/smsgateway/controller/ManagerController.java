package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ChangePasswordManager;
import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.service.ManagerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/managers")
@Tag(name = "Managers", description = "Endpoints for managing manager accounts (creation, activation, update, delete)")
public class ManagerController {

    @Autowired
    private ManagerService managerService;
    @Autowired
    private ManagerRepository managerRepository;


    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Create a manager account", tags = "Managers")
    public ResponseEntity<ManagerDTO> create(@Valid @RequestBody ManagerDTO body) {
        ManagerDTO manager = managerService.create(body);
        return ResponseEntity
                .created(URI.create("/api/V1/managers/" + manager.getIdManager()))
                .body(manager);
    }

    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordManager body,
            Authentication authentication) {

        String connectedEmail = authentication.getName();
        ManagerDTO manager = managerService.findById(id);

        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!manager.getEmail().equalsIgnoreCase(connectedEmail) && !isSuperAdmin) {
            throw new AccessDeniedException("Vous ne pouvez modifier que votre propre mot de passe");
        }

        managerService.changePassword(id, body.getOldPassword(), body.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Mot de passe modifié avec succès"
        ));
    }



    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Get all managers", tags = "Managers")
    public ResponseEntity<List<ManagerDTO>> findAll() {
        return ResponseEntity.ok(managerService.findAll());
    }



    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Get manager by ID", tags = "Managers")
    public ResponseEntity<ManagerDTO> findById(@PathVariable String id) {
        return ResponseEntity.ok(managerService.findById(id));
    }

    /** Partial update: send only the fields to modify */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Update manager (partial)", tags = "Managers")
    public ResponseEntity<ManagerDTO> patch(@PathVariable String id, @RequestBody ManagerDTO body) {
        return ResponseEntity.ok(managerService.patch(id, body));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Delete a manager by ID", tags = "Managers")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }
    /** ⏸ Suspendre un manager */
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Suspendre un manager")
    public ResponseEntity<Map<String, String>> suspend(@PathVariable String id) {
        return ResponseEntity.ok(managerService.suspendManager(id));
    }

    /** ✅ Réactiver un manager */
    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Réactiver un manager")
    public ResponseEntity<Map<String, String>> reactivate(@PathVariable String id) {
        return ResponseEntity.ok(managerService.reactivateManager(id));
    }

    /** 🗄 Archiver un manager */
    @PutMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Archiver un manager (soft delete)")
    public ResponseEntity<Map<String, String>> archive(@PathVariable String id) {
        return ResponseEntity.ok(managerService.archiveManager(id));
    }
    /** 🔄 Désarchiver un manager */
    @PutMapping("/{id}/unarchive")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    @Operation(summary = "Désarchiver un manager (le remettre ACTIF)")
    public ResponseEntity<Map<String, String>> unarchive(@PathVariable String id) {
        return ResponseEntity.ok(managerService.unarchiveManager(id));
    }


}
