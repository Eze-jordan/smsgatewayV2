package com.ogooueTech.smsgateway.service;


import com.ogooueTech.smsgateway.model.Facture;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
@Service
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;
    private final FooterInfoService footerInfoService; // ✅ service du footer

    public InvoicePdfService(SpringTemplateEngine templateEngine,
                             FooterInfoService footerInfoService) {
        this.templateEngine = templateEngine;
        this.footerInfoService = footerInfoService;
    }

    public byte[] renderInvoice(Facture f) {
        var ctx = new Context();
        var df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        var pf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String invoiceNumber = "INV-" + f.getId().substring(0, 8).toUpperCase();

        // ====== Variables de facture ======
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

        // ====== Variables du footer (depuis la DB) ======
        var footer = footerInfoService.getFooterInfo();
        ctx.setVariable("companyName", footer.getCompanyName());
        ctx.setVariable("companyAddress", footer.getCompanyAddress());
        ctx.setVariable("companyNif", footer.getCompanyNif());
        ctx.setVariable("companyRccm", footer.getCompanyRccm());
        ctx.setVariable("companyEmail", footer.getCompanyEmail());
        ctx.setVariable("companyPhone", footer.getCompanyPhone());
        ctx.setVariable("paymentNote", footer.getPaymentNote());

        // ====== Génération HTML + PDF ======
        String html = templateEngine.process("invoice", ctx);

        try (var out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // baseUri → permet de résoudre /logo.png dans src/main/resources/static
            builder.withHtmlContent(html, new File("src/main/resources/static/").toURI().toString());
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lors de la génération PDF: " + e.getMessage(), e);
        }

    }

    private String toCurrencyNoDecimals(BigDecimal v) {
        if (v == null) return "0";
        return v.setScale(0).toPlainString();
    }
}
