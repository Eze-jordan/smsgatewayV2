package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ForgotPasswordRequest;
import com.ogooueTech.smsgateway.dtos.ResetPasswordRequest;
import com.ogooueTech.smsgateway.service.ClientPasswordResetService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/password")
@Tag(name = "Client Password Reset", description = "Endpoints for client password recovery and reset")
public class PasswordResetController {

    private final ClientPasswordResetService resetService;

    public PasswordResetController(ClientPasswordResetService resetService) {
        this.resetService = resetService;
    }

    /** Step 1: Forgot password */
    @PostMapping("/forgot")
    @Operation(summary = "Request password reset (forgot password)", tags = "Client Password Reset")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        // Generic response = prevents email enumeration
        try {
            resetService.forgotPassword(request);
        } catch (Exception ignored) {}
        return ResponseEntity.ok("If an account exists for this email, a reset link has been sent.");
    }

    /** Step 2: Reset password with token */
    @PostMapping("/reset")
    @Operation(summary = "Reset client password with token", tags = "Client Password Reset")
    public ResponseEntity<?> reset(
            @RequestParam String token,                  // token in the URL
            @Valid @RequestBody ResetPasswordRequest request // newPassword in the body
    ) {
        try {
            resetService.resetPassword(token, request.getNewPassword());
            return ResponseEntity.ok("Client password successfully reset.");
        } catch (IllegalArgumentException | EntityNotFoundException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
        }
    }
}
