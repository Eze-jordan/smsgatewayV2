package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.MuldespRequest;
import com.ogooueTech.smsgateway.dtos.SmsMuldesResponse;
import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import com.ogooueTech.smsgateway.model.SmsMessage;
import com.ogooueTech.smsgateway.service.SmsService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping(path = "/api/V1/sms", produces = "application/json")
@Tag(name = "SMS", description = "Endpoints to send SMS (single, bulk, scheduled) and to query history")
public class SmsController {

    private final SmsService smsService;
    public SmsController(SmsService smsService) { this.smsService = smsService; }

    @PostMapping(path = "/unides", consumes = "application/json")
    @Operation(summary = "Send a single SMS (UNIDES)", tags = "SMS")
    public ResponseEntity<?> unides(@RequestHeader("X-API-Key") String apiKey,
                                    @RequestBody UnidesRequest req) {
        try {
            if (req == null || isBlank(req.clientId()) || isBlank(req.emetteur())
                    || isBlank(req.destinataire()) || isBlank(req.corps())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            var client = smsService.assertApiKey(apiKey, req.clientId());

            SmsMessage sms = smsService.createUnides(
                    client.getIdclients(), req.emetteur(), req.destinataire(), req.corps()
            );
            smsService.envoyerImmediate(sms);

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of(
                            "ref", sms.getRef(),
                            "type", SmsType.UNIDES.name(),
                            "statut", sms.getStatut().name()
                    ));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            int status = ("Clé API requise".equals(msg) || "Clé API invalide".equals(msg) || msg.startsWith("Clé API non"))
                    ? 401 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error", "details", ex.getMessage()));
        }
    }

    @PostMapping(path = "/muldes", consumes = "application/json")
    @Operation(summary = "Send bulk SMS immediately (MULDES)", tags = "SMS")
    public ResponseEntity<?> muldes(@RequestHeader("X-API-Key") String apiKey,
                                    @RequestBody MuldesRequest req) {
        try {
            if (req == null || isBlank(req.clientId()) || isBlank(req.emetteur())
                    || req.numeros() == null || req.numeros().isEmpty()
                    || isBlank(req.corps())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            var client = smsService.assertApiKey(apiKey, req.clientId());
            var sms = smsService.createMuldes(client.getIdclients(), req.emetteur(), req.numeros(), req.corps());
            smsService.envoyerImmediate(sms);

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of("ref", sms.getRef(), "type", SmsType.MULDES.name(), "statut", sms.getStatut().name()));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            int status = ("Clé API requise".equals(msg) || "Clé API invalide".equals(msg) || msg.startsWith("Clé API non"))
                    ? 401 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error", "details", ex.getMessage()));
        }
    }

    @Operation(summary = "Get all PENDING SMS", description = "Returns all SMS with status = EN_ATTENTE")
    @GetMapping("/pending")
    public ResponseEntity<List<SmsMessage>> getPendingMessages() {
        List<SmsMessage> pending = smsService.getAllPendingMessages();
        return ResponseEntity.ok(pending);
    }

    @Operation(summary = "Mark SMS as SENT", description = "Change status of a specific SMS (and its recipients) from EN_ATTENTE to ENVOYE")
    @PutMapping("/{ref}/mark-sent")
    public ResponseEntity<String> markAsSent(@PathVariable String ref) {
        smsService.markAsSent(ref);
        return ResponseEntity.ok("✅ SMS " + ref + " marked as SENT.");
    }

    // 🔹 Récupérer tous les SMS envoyés (liste simple)
    @GetMapping("/envoyes")
    @Operation(summary = "Lister tous les SMS envoyés", description = "Retourne tous les SMS avec statut = ENVOYE")
    public ResponseEntity<List<SmsMessage>> getAllSmsEnvoyes() {
        return ResponseEntity.ok(smsService.getAllSmsEnvoyes());
    }

    @PostMapping(path = "/muldesp", consumes = "application/json")
    @Operation(summary = "Schedule bulk SMS over a period (MULDESP)", tags = "SMS")
    public ResponseEntity<?> muldesp(@RequestHeader("X-API-Key") String apiKey,
                                     @RequestBody MuldespRequest req) {
        try {
            if (req == null || isBlank(req.clientId()) || isBlank(req.emetteur())
                    || req.numeros() == null || req.numeros().isEmpty()
                    || isBlank(req.corps())
                    || req.dateDebut() == null || req.dateFin() == null
                    || req.nbParJour() == null || req.intervalleMinutes() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing scheduling parameters"));
            }

            var client = smsService.assertApiKey(apiKey, req.clientId());

            SmsMessage sms = smsService.createMuldesp(
                    client.getIdclients(),
                    req.emetteur(),
                    req.numeros(),
                    req.corps(),
                    req.dateDebut(),     // LocalDate
                    req.nbParJour(),
                    req.intervalleMinutes(),
                    req.dateFin()        // LocalDate
            );


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
            String msg = ex.getMessage();
            int status = ("Clé API requise".equals(msg) || "Clé API invalide".equals(msg) || msg.startsWith("Clé API non"))
                    ? 401 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error", "details", ex.getMessage()));
        }
    }


    /* ===== DTOs ===== */
    public record UnidesRequest(String clientId, String emetteur, String destinataire, String corps) {}
    public record MuldesRequest(String clientId, String emetteur, List<String> numeros, String corps) {}

    /* ===== Helpers ===== */
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Operation(summary = "Handle missing headers (maps missing X-API-Key to 401)", tags = "SMS")
    public ResponseEntity<?> onMissingHeader(MissingRequestHeaderException ex) {
        if ("X-API-Key".equals(ex.getHeaderName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Clé API requise"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @GetMapping("/user/{numero}")
    @Operation(summary = "Get SMS sent to a given recipient number", tags = "SMS")
    public ResponseEntity<List<SmsMessage>> getSmsByDestinataire(@PathVariable String numero) {
        return ResponseEntity.ok(smsService.getSmsEnvoyesByDestinataire(numero));
    }

    @GetMapping("/client/{clientId}/unides")
    @Operation(summary = "List UNIDES SMS by client with optional filters", tags = "SMS")
    public ResponseEntity<Page<SmsMessage>> getUnidesByClient(
            @PathVariable String clientId,
            @RequestParam(required = false) SmsStatus statut,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        if (start != null && end != null) {
            return ResponseEntity.ok(
                    smsService.getUnidesByClientBetweenDates(clientId, start, end, statut, page, size)
            );
        }
        if (statut != null) {
            return ResponseEntity.ok(
                    smsService.getUnidesByClientAndStatut(clientId, statut, page, size)
            );
        }
        return ResponseEntity.ok(smsService.getUnidesByClient(clientId, page, size));
    }

    @GetMapping("/client/{clientId}/muldes")
    @Operation(summary = "List MULDES SMS by client with optional status filter", tags = "SMS")
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
    @Operation(summary = "List MULDES SMS with parsed recipients by client", tags = "SMS")
    public ResponseEntity<Page<SmsMuldesResponse>> getMuldesWithRecipients(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer size,
            @RequestParam(required = false) SmsStatus statut
    ) {
        return ResponseEntity.ok(smsService.getMuldesWithParsedRecipients(clientId, page, size, statut));
    }

    // ✅ Récupérer tous les UNIDES
    @GetMapping("/unides/{clientId}")
    @Operation(summary = "Get all UNIDES SMS", description = "Retrieve all single-destination SMS for a given client")
    public List<SmsMessage> getAllUnides(@PathVariable String clientId) {
        return smsService.getAllUnides(clientId);
    }

    // ✅ Récupérer tous les MULDES
    @GetMapping("/muldes/{clientId}")
    @Operation(summary = "Get all MULDES SMS", description = "Retrieve all multi-destination SMS for a given client with recipients")
    public List<SmsMuldesResponse> getAllMuldes(@PathVariable String clientId) {
        return smsService.getAllMuldes(clientId);
    }

    // ✅ Récupérer tous les MULDESP (planifiés)
    @GetMapping("/muldesp/{clientId}")
    @Operation(summary = "Get all MULDESP SMS", description = "Retrieve all scheduled multi-destination SMS for a given client with recipients")
    public List<SmsMuldesResponse> getAllMuldesp(@PathVariable String clientId) {
        return smsService.getAllMuldesp(clientId);
    }
}
