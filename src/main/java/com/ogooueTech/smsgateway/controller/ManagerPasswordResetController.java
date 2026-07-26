package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.ResetPasswordRequest;
import com.ogooueTech.smsgateway.exception.ManagerNotFoundException;
import com.ogooueTech.smsgateway.service.ManagerPasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/V1/manager/password")
@Tag(
        name = "Manager Password Reset",
        description = "Endpoints for manager password recovery and reset"
)
public class ManagerPasswordResetController {

    private final ManagerPasswordResetService resetService;

    public ManagerPasswordResetController(
            ManagerPasswordResetService resetService
    ) {
        this.resetService = resetService;
    }

    /**
     * Demande d'un lien de réinitialisation.
     */
    @PostMapping("/forgot")
    @Operation(
            summary = "Request password reset (forgot password)",
            tags = "Manager Password Reset"
    )
    public ResponseEntity<?> forgot(
            @RequestBody Map<String, String> body
    ) {
        try {
            String email = body.get("email");

            if (email == null || email.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error", "BAD_REQUEST",
                                "message",
                                "L'adresse e-mail est obligatoire",
                                "status", 400
                        ));
            }

            resetService.forgotPassword(email.trim());

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Un lien de réinitialisation a été envoyé"
                    )
            );

        } catch (
                ManagerNotFoundException |
                EntityNotFoundException exception
        ) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error", "MANAGER_NOT_FOUND",
                            "message", exception.getMessage(),
                            "status", 404
                    ));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "BAD_REQUEST",
                            "message", exception.getMessage(),
                            "status", 400
                    ));

        } catch (Exception exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "INTERNAL_ERROR",
                            "message",
                            "Une erreur inattendue est survenue",
                            "status", 500
                    ));
        }
    }

    /**
     * Réinitialise le mot de passe avec le token reçu.
     */
    @PostMapping("/reset")
    @Operation(
            summary = "Reset manager password with token",
            tags = "Manager Password Reset"
    )
    public ResponseEntity<?> reset(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            resetService.resetPassword(
                    token,
                    request.getNewPassword()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Manager password successfully reset."
                    )
            );

        } catch (
                IllegalArgumentException |
                EntityNotFoundException exception
        ) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error", "BAD_REQUEST",
                            "message", exception.getMessage(),
                            "status", 400
                    ));

        } catch (Exception exception) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "INTERNAL_ERROR",
                            "message",
                            "Une erreur inattendue est survenue",
                            "status", 500
                    ));
        }
    }
}