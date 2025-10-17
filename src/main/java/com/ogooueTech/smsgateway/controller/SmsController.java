package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.MuldespRequest;
import com.ogooueTech.smsgateway.dtos.SmsMuldesResponse;
import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.SmsMessage;
import com.ogooueTech.smsgateway.service.SmsService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/api/V1/sms", produces = "application/json")
@Tag(name = "SMS", description = "Endpoints to send SMS (single, bulk, scheduled) and to query history")
public class SmsController {

    private final SmsService smsService;

    public SmsController(SmsService smsService) {
        this.smsService = smsService;
    }

    /* ===============================================
       ===============  ENVOIS SMS  ===================
       =============================================== */

    @PostMapping(path = "/unides", consumes = "application/json")
    @Operation(summary = "Send a single SMS (UNIDES)", tags = "SMS")
    public ResponseEntity<?> unides(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody UnidesRequest req) {
        try {
            if (req == null || isBlank(req.emetteur()) || isBlank(req.destinataire()) || isBlank(req.corps())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            SmsMessage sms;

            // ✅ Mode externe : clé API fournie
            if (apiKey != null && !apiKey.isBlank()) {
                Client client = smsService.assertApiKey(apiKey);
                sms = smsService.createUnides(client.getIdclients(), req.emetteur(), req.destinataire(), req.corps());
            }
            // ✅ Mode interne : espace client (pas de clé)
            else {
                if (isBlank(req.clientId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "clientId requis pour le mode interne"));
                }
                sms = smsService.createUnides(req.clientId(), req.emetteur(), req.destinataire(), req.corps());
            }

            smsService.envoyerImmediate(sms);

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of(
                            "ref", sms.getRef(),
                            "type", SmsType.UNIDES.name(),
                            "statut", sms.getStatut().name()
                    ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/muldes", consumes = "application/json")
    @Operation(summary = "Send bulk SMS immediately (MULDES)", tags = "SMS")
    public ResponseEntity<?> muldes(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody MuldesRequest req) {
        try {
            if (req == null || isBlank(req.emetteur()) || req.numeros() == null || req.numeros().isEmpty() || isBlank(req.corps())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            SmsMessage sms;

            // ✅ Mode externe
            if (apiKey != null && !apiKey.isBlank()) {
                Client client = smsService.assertApiKey(apiKey);
                sms = smsService.createMuldes(client.getIdclients(), req.emetteur(), req.numeros(), req.corps());
            }
            // ✅ Mode interne
            else {
                if (isBlank(req.clientId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "clientId requis pour le mode interne"));
                }
                sms = smsService.createMuldes(req.clientId(), req.emetteur(), req.numeros(), req.corps());
            }

            smsService.envoyerImmediate(sms);
            return ResponseEntity.created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of("ref", sms.getRef(), "type", SmsType.MULDES.name(), "statut", sms.getStatut().name()));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping(path = "/muldesp", consumes = "application/json")
    @Operation(summary = "Schedule bulk SMS over a period (MULDESP)", tags = "SMS")
    public ResponseEntity<?> muldesp(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody MuldespRequest req) {
        try {
            if (req == null || isBlank(req.emetteur())
                    || req.numeros() == null || req.numeros().isEmpty()
                    || isBlank(req.corps())
                    || req.dateDebut() == null || req.dateFin() == null
                    || req.nbParJour() == null || req.intervalleMinutes() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing scheduling parameters"));
            }

            SmsMessage sms;

            // ✅ Mode externe
            if (apiKey != null && !apiKey.isBlank()) {
                Client client = smsService.assertApiKey(apiKey);
                sms = smsService.createMuldesp(client.getIdclients(), req.emetteur(), req.numeros(),
                        req.corps(), req.dateDebut(), req.nbParJour(), req.intervalleMinutes(), req.dateFin());
            }
            // ✅ Mode interne
            else {
                if (isBlank(req.clientId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "clientId requis pour le mode interne"));
                }
                sms = smsService.createMuldesp(req.clientId(), req.emetteur(), req.numeros(),
                        req.corps(), req.dateDebut(), req.nbParJour(), req.intervalleMinutes(), req.dateFin());
            }

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of(
                            "ref", sms.getRef(),
                            "type", SmsType.MULDESP.name(),
                            "statut", sms.getStatut().name(),
                            "dateDebut", req.dateDebut(),
                            "dateFin", req.dateFin(),
                            "nbParJour", sms.getNbParJour(),
                            "intervalleMinutes", sms.getIntervalleMinutes()
                    ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage()));
        }
    }

    /* ===============================================
       ===============  CONSULTATION  =================
       =============================================== */

    /**
     * Récupère tous les SMS en attente avec leurs destinataires
     */
    @GetMapping("/pending")
    @Operation(summary = "Obtenir tous les SMS en attente",
            description = "Retourne tous les SMS en statut EN_ATTENTE avec leurs destinataires, filtrés selon les règles métier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des SMS en attente récupérée avec succès"),
            @ApiResponse(responseCode = "204", description = "Aucun SMS en attente trouvé"),
            @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
    })
    public ResponseEntity<List<SmsService.PendingSmsDetails>> getAllPendingSms() {
        List<SmsService.PendingSmsDetails> pendingSms = smsService.getPendingSmsOptimized();

        if (pendingSms.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(pendingSms);
    }


    /**
     * Récupère les statistiques des SMS en attente
     */
    @GetMapping("/stats")
    @Operation(summary = "Obtenir les statistiques des SMS en attente",
            description = "Retourne les statistiques globales des SMS en attente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistiques récupérées avec succès")
    })
    public ResponseEntity<Map<String, Object>> getPendingSmsStats() {
        List<SmsService.PendingSmsDetails> pendingSms = smsService.getPendingSmsOptimized();

        Map<String, Object> stats = Map.of(
                "totalSms", pendingSms.size(),
                "totalDestinataires", pendingSms.stream().mapToInt(SmsService.PendingSmsDetails::getTotalRecipients).sum(),
                "parType", pendingSms.stream().collect(Collectors.groupingBy(
                        details -> details.getSms().getType().name(),
                        Collectors.summarizingInt(SmsService.PendingSmsDetails::getTotalRecipients)
                )),
                "details", pendingSms.stream().map(details -> Map.of(
                        "ref", details.getSms().getRef(),
                        "type", details.getSms().getType(),
                        "destinataires", details.getTotalRecipients(),
                        "clientId", details.getSms().getClientId(),
                        "dateCreation", details.getSms().getCreatedAt()
                )).collect(Collectors.toList())
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * Récupère les SMS en attente pour un client spécifique
     */
    @GetMapping("/client/{clientId}")
    @Operation(summary = "Obtenir les SMS en attente d'un client",
            description = "Retourne tous les SMS en attente pour un client spécifique")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS du client récupérés avec succès"),
            @ApiResponse(responseCode = "204", description = "Aucun SMS en attente pour ce client")
    })
    public ResponseEntity<List<SmsService.PendingSmsDetails>> getPendingSmsByClient(
            @Parameter(description = "ID du client", required = true, example = "client-123")
            @PathVariable String clientId) {

        List<SmsService.PendingSmsDetails> allPending = smsService.getPendingSmsOptimized();
        List<SmsService.PendingSmsDetails> clientPending = allPending.stream()
                .filter(details -> clientId.equals(details.getSms().getClientId()))
                .collect(Collectors.toList());

        if (clientPending.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(clientPending);
    }

    @PutMapping("/{ref}/mark-sent")
    public ResponseEntity<String> markAsSent(@PathVariable String ref) {
        smsService.markAsSent(ref);
        return ResponseEntity.ok("✅ SMS " + ref + " marked as SENT.");
    }

    @GetMapping("/envoyes")
    public ResponseEntity<List<SmsMessage>> getAllSmsEnvoyes() {
        return ResponseEntity.ok(smsService.getAllSmsEnvoyes());
    }

    @GetMapping("/user/{numero}")
    public ResponseEntity<List<SmsMessage>> getSmsByDestinataire(@PathVariable String numero) {
        return ResponseEntity.ok(smsService.getSmsEnvoyesByDestinataire(numero));
    }

    @GetMapping("/client/{clientId}/unides")
    public ResponseEntity<Page<SmsMessage>> getUnidesByClient(
            @PathVariable String clientId,
            @RequestParam(required = false) SmsStatus statut,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        if (start != null && end != null) {
            return ResponseEntity.ok(smsService.getUnidesByClientBetweenDates(clientId, start, end, statut, page, size));
        }
        if (statut != null) {
            return ResponseEntity.ok(smsService.getUnidesByClientAndStatut(clientId, statut, page, size));
        }
        return ResponseEntity.ok(smsService.getUnidesByClient(clientId, page, size));
    }

    @GetMapping("/client/{clientId}/muldes")
    public ResponseEntity<Page<SmsMessage>> getMuldesByClient(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) SmsStatus statut
    ) {
        if (statut != null) {
            return ResponseEntity.ok(smsService.getMuldesByClientAndStatut(clientId, statut, page, size));
        }
        return ResponseEntity.ok(smsService.getMuldesByClient(clientId, page, size));
    }

    @GetMapping("/client/{clientId}/muldes-with-recipients")
    public ResponseEntity<Page<SmsMuldesResponse>> getMuldesWithRecipients(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(required = false) SmsStatus statut
    ) {
        return ResponseEntity.ok(smsService.getMuldesWithParsedRecipients(clientId, page, size, statut));
    }

    @GetMapping("/unides/{clientId}")
    public List<SmsMessage> getAllUnides(@PathVariable String clientId) {
        return smsService.getAllUnides(clientId);
    }

    @GetMapping("/muldes/{clientId}")
    public List<SmsMuldesResponse> getAllMuldes(@PathVariable String clientId) {
        return smsService.getAllMuldes(clientId);
    }

    @GetMapping("/muldesp/{clientId}")
    public List<SmsMuldesResponse> getAllMuldesp(@PathVariable String clientId) {
        return smsService.getAllMuldesp(clientId);
    }

    /* ===== DTOs internes ===== */
    public record UnidesRequest(String clientId, String emetteur, String destinataire, String corps) {}
    public record MuldesRequest(String clientId, String emetteur, List<String> numeros, String corps) {}

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
