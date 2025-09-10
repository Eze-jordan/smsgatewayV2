package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.service.ManagerService;
import com.ogooueTech.smsgateway.service.ValidationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Autowired
    private ValidationService validationService;

    @PostMapping("/create")
    @Operation(summary = "Create a manager account", tags = "Managers")
    public ResponseEntity<ManagerDTO> create(@Valid @RequestBody ManagerDTO body) {
        ManagerDTO manager = managerService.create(body);
        return ResponseEntity
                .created(URI.create("/api/managers/" + manager.getIdManager()))
                .body(manager);
    }

    @PostMapping("/activation")
    @Operation(
            summary = "Activate a manager account",
            description = "Activate a manager account via an OTP code.",
            tags = "Managers"
    )
    public ResponseEntity<String> activation(@RequestBody Map<String, String> activation) {
        try {
            this.managerService.activation(activation);
            return ResponseEntity.ok("Account successfully activated.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Get all managers", tags = "Managers")
    public ResponseEntity<List<ManagerDTO>> findAll() {
        return ResponseEntity.ok(managerService.findAll());
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP code for activation", tags = "Managers")
    public ResponseEntity<?> resendOtp(@RequestBody ManagerDTO dto) {
        Manager manager = managerRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        validationService.renvoyerCode(manager);
        return ResponseEntity.ok("New OTP sent.");
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

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a manager by ID", tags = "Managers")
    public ResponseEntity<ManagerDTO> activate(@PathVariable String id) {
        return ResponseEntity.ok(managerService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a manager by ID", tags = "Managers")
    public ResponseEntity<ManagerDTO> deactivate(@PathVariable String id) {
        return ResponseEntity.ok(managerService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a manager by ID", tags = "Managers")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
