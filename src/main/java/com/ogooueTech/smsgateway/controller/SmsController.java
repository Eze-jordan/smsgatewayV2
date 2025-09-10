package com.ogooueTech.smsgateway.controller;

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

            SmsMessage sms = smsService.createUnides(client.getIdclients(), req.emetteur(), req.destinataire(), req.corps());
            smsService.envoyerImmediate(sms);

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of("ref", sms.getRef(), "type", SmsType.UNIDES.name(), "statut", sms.getStatut().name()));
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

    @PostMapping(path = "/muldesp", consumes = "application/json")
    @Operation(summary = "Schedule bulk SMS over a period (MULDESP)", tags = "SMS")
    public ResponseEntity<?> muldesp(@RequestHeader("X-API-Key") String apiKey,
                                     @RequestBody MuldespRequest req) {
        try {
            if (req == null || isBlank(req.clientId()) || isBlank(req.emetteur())
                    || req.numeros() == null || req.numeros().isEmpty()
                    || isBlank(req.corps())
                    || req.dateDebut() == null || req.nbParJour() == null
                    || req.intervalleMinutes() == null || req.dateFin() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing scheduling parameters"));
            }

            var client = smsService.assertApiKey(apiKey, req.clientId());

            SmsMessage sms = smsService.createMuldesp(
                    client.getIdclients(), req.emetteur(), req.numeros(), req.corps(),
                    req.dateDebut(), req.nbParJour(), req.intervalleMinutes(), req.dateFin()
            );

            return ResponseEntity
                    .created(URI.create("/api/V1/sms/" + sms.getRef()))
                    .body(Map.of(
                            "ref", sms.getRef(),
                            "type", SmsType.MULDESP.name(),
                            "statut", sms.getStatut().name(),
                            "prochaineHeureEnvoi", sms.getProchaineHeureEnvoi()
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
    public record MuldespRequest(
            String clientId,
            String emetteur,
            List<String> numeros,
            String corps,
            LocalDateTime dateDebut,
            Integer nbParJour,
            Integer intervalleMinutes,
            LocalDateTime dateFin
    ) {}

    /* ===== Helpers ===== */
    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** Convert missing X-API-Key header into 401 instead of generic 400. */
//    @ExceptionHandler(MissingRequestHeaderException.class)
    @Operation(summary = "Handle missing headers (maps missing X-API-Key to 401)", tags = "SMS")
    public ResponseEntity<?> onMissingHeader(MissingRequestHeaderException ex) {
        if ("X-API-Key".equals(ex.getHeaderName())) {
            return ResponseEntity.status(401).body(Map.of("error", "Clé API requise"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    // Example: GET /api/v1/sms-messages/user/24106234567
    @GetMapping("/user/{numero}")
    @Operation(summary = "Get SMS sent to a given recipient number", tags = "SMS")
    public ResponseEntity<List<SmsMessage>> getSmsByDestinataire(@PathVariable String numero) {
        return ResponseEntity.ok(smsService.getSmsEnvoyesByDestinataire(numero));
    }

    // GET /api/V1/sms/client/{clientId}/unides?page=0&size=20
    // Optional filters: &statut=ENVOYE&start=2025-09-01T00:00:00&end=2025-09-04T23:59:59
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

    // GET /api/V1/sms/client/{clientId}/muldes?page=0&size=20
    // Optional: &statut=ENVOYE
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

    // GET /api/V1/sms/client/{clientId}/muldes-with-recipients?page=0&size=20
    // Optional: &statut=ENVOYE
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
}
