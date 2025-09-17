package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.FactureDTO;
import com.ogooueTech.smsgateway.model.Facture;
import com.ogooueTech.smsgateway.service.FacturationService;
import com.ogooueTech.smsgateway.service.InvoiceAppService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/V1/billing/factures")
@Tag(name = "Invoices", description = "Endpoints for invoice management (PDF preview/download, send by email)")
public class InvoiceController {

    private final InvoiceAppService invoiceAppService;
    private final FacturationService facturationService;

    public InvoiceController(InvoiceAppService invoiceAppService, FacturationService facturationService) {
        this.invoiceAppService = invoiceAppService;
        this.facturationService = facturationService;
    }

    /**
     * Preview or download an invoice as PDF
     */
    @GetMapping("/{id}/pdf")
    @Operation(summary = "Get invoice PDF (preview/download)", tags = "Invoices")
    public ResponseEntity<byte[]> getPdf(@PathVariable String id) {
        byte[] pdf = invoiceAppService.generatePdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    /**
     * Generate and send the invoice PDF by email to the client
     */
    @PostMapping("/{id}/send")
    @Operation(summary = "Send invoice by email", tags = "Invoices")
    public ResponseEntity<String> sendByEmail(@PathVariable String id) {
        invoiceAppService.sendPdfByEmail(id);
        return ResponseEntity.ok("Invoice sent by email.");
    }

    /**
     * 🔹 Récupérer toutes les factures
     */

    @GetMapping
    public ResponseEntity<List<FactureDTO>> getAllFactures() {
        return ResponseEntity.ok(
                facturationService.getAllFactures()
                        .stream()
                        .map(FactureDTO::from)
                        .toList()
        );
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FactureDTO>> getFacturesByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(facturationService.getFacturesByClient(clientId));
    }

    @GetMapping("/client/{clientId}/periode")
    public ResponseEntity<List<FactureDTO>> getFacturesByClientAndDateRange(
            @PathVariable String clientId,
            @RequestParam LocalDate start,
            @RequestParam LocalDate end
    ) {
        return ResponseEntity.ok(facturationService.getFacturesByClientAndDateRange(clientId, start, end));
    }

}
