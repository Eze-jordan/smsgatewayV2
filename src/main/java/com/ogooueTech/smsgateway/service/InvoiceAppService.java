package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class InvoiceAppService {

    private final FactureRepository factureRepository;
    private final InvoicePdfService invoicePdfService;
    private final NotificationService notificationService; // <-- utilise ton service

    public InvoiceAppService(FactureRepository factureRepository, InvoicePdfService invoicePdfService, NotificationService notificationService) {
        this.factureRepository = factureRepository;
        this.invoicePdfService = invoicePdfService;
        this.notificationService = notificationService;
    }

    public byte[] generatePdf(String factureId) {
        var f = factureRepository.findDetailById(factureId)
                .orElseThrow(() -> new IllegalStateException("Facture introuvable: " + factureId));
        return invoicePdfService.renderInvoice(f);
    }

    public void sendPdfByEmail(String factureId) {
        var f = factureRepository.findDetailById(factureId)
                .orElseThrow(() -> new IllegalStateException("Facture introuvable: " + factureId));
        byte[] pdf = invoicePdfService.renderInvoice(f);
        notificationService.envoyerFacture(f, pdf); // <-- envoi via ta classe
    }
}
