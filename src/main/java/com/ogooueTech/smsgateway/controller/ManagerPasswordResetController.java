package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ResetPasswordRequest;
import com.ogooueTech.smsgateway.service.ManagerPasswordResetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/manager/password")
@Tag(name = "Manager Password Reset", description = "Endpoints for manager password recovery and reset")
public class ManagerPasswordResetController {

    private final ManagerPasswordResetService resetService;

    public ManagerPasswordResetController(ManagerPasswordResetService resetService) {
        this.resetService = resetService;
    }

    @PostMapping("/forgot")
    @Operation(summary = "Request password reset (forgot password)", tags = "Manager Password Reset")
    public ResponseEntity<?> forgot(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        resetService.forgotPassword(email);
        return ResponseEntity.ok("If a manager account exists for this email, a reset link has been sent.");
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset manager password with token", tags = "Manager Password Reset")
    public ResponseEntity<?> reset(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            resetService.resetPassword(token, request.getNewPassword());
            return ResponseEntity.ok("Manager password successfully reset.");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
        }
    }
}
