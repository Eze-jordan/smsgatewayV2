package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Facture;
import com.ogooueTech.smsgateway.repository.FactureRepository;
import org.springframework.stereotype.Service;
/**
 * Service applicatif chargé :
 * - de récupérer une facture complète ;
 * - de générer son document PDF ;
 * - d'envoyer le document au client.
 */
@Service
public class InvoiceAppService {

    private final FactureRepository factureRepository;
    private final com.ogooueTech.smsgateway.service.InvoicePdfService invoicePdfService;
    private final com.ogooueTech.smsgateway.service.NotificationService notificationService;

    /**
     * Injection des dépendances.
     *
     * @param factureRepository repository des factures
     * @param invoicePdfService service de génération PDF
     * @param notificationService service d'envoi des notifications
     */
    public InvoiceAppService(FactureRepository factureRepository, com.ogooueTech.smsgateway.service.InvoicePdfService invoicePdfService, com.ogooueTech.smsgateway.service.NotificationService notificationService) {
        this.factureRepository = factureRepository;
        this.invoicePdfService = invoicePdfService;
        this.notificationService = notificationService;
    }

    /**
     * Génère le PDF d'une facture.
     *
     * @param factureId identifiant de la facture
     * @return contenu binaire du PDF
     */
    public byte[] generatePdf(String factureId) {
        var f = factureRepository.findDetailById(factureId)
                .orElseThrow(() -> new IllegalStateException("Facture introuvable: " + factureId));
        return invoicePdfService.renderInvoice(f);
    }
    /**
     * Génère puis envoie le PDF d'une facture par e-mail.
     *
     * @param factureId identifiant de la facture
     */
    public void sendPdfByEmail(String factureId) {
        var f = factureRepository.findDetailById(factureId)
                .orElseThrow(() -> new IllegalStateException("Facture introuvable: " + factureId));
        byte[] pdf = invoicePdfService.renderInvoice(f);
        notificationService.envoyerFacture(f, pdf); // <-- envoi via ta classe
    }

    /**
     * Recherche une facture avec son client et son exercice.
     */
    private Facture rechercherFacture(String factureId) {
        if (factureId == null || factureId.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identifiant de la facture est obligatoire"
            );
        }

        return factureRepository
                .findDetailById(factureId.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Facture introuvable"
                ));
    }
}
