package com.ogooueTech.smsgateway.service;


import com.ogooueTech.smsgateway.model.Facture;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;

    public InvoicePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] renderInvoice(Facture f) {
        var ctx = new Context();
        var df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var pf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String invoiceNumber = "INV-" + f.getId().substring(0, 8).toUpperCase();

        ctx.setVariable("title", "Facture " + invoiceNumber);
        ctx.setVariable("invoiceNumber", invoiceNumber);
        ctx.setVariable("dateFacture", f.getDateFacture().format(df));
        ctx.setVariable("annee", f.getExercice().getAnnee());
        ctx.setVariable("periode", f.getDateDebut().format(pf) + " - " + f.getDateFin().format(pf));

        ctx.setVariable("clientRaison", f.getClient().getRaisonSociale());
        ctx.setVariable("clientEmail", f.getClient().getEmail());
        ctx.setVariable("emetteur", f.getClient().getEmetteur());

        ctx.setVariable("consommation", f.getConsommationSms());
        ctx.setVariable("prixUnitaire", toCurrencyNoDecimals(f.getPrixUnitaire()));
        ctx.setVariable("montant", toCurrencyNoDecimals(f.getMontant()));

        String html = templateEngine.process("invoice", ctx);

        try (var out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la génération PDF: " + e.getMessage(), e);
        }
    }

    private String toCurrencyNoDecimals(BigDecimal v) {
        if (v == null) return "0";
        // FCFA: pas de décimales
        return v.setScale(0).toPlainString();
    }
}
