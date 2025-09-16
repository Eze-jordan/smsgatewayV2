package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.repository.FactureRepository;
import org.springframework.stereotype.Service;

@Service
public class InvoiceAppService {

    private final FactureRepository factureRepository;
    private final com.ogooueTech.smsgateway.service.InvoicePdfService invoicePdfService;
    private final com.ogooueTech.smsgateway.service.NotificationService notificationService; // <-- utilise ton service

    public InvoiceAppService(FactureRepository factureRepository, com.ogooueTech.smsgateway.service.InvoicePdfService invoicePdfService, com.ogooueTech.smsgateway.service.NotificationService notificationService) {
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
