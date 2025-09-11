package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.model.Validation;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
// imports utiles à ajouter :
import com.ogooueTech.smsgateway.model.Facture;
import org.springframework.core.io.ByteArrayResource;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class NotificationService {
    JavaMailSender javaMailSender;
    // 🔑 injection de l'URL du front (par défaut = app.mondomaine.com si rien n’est défini)
    @Value("${app.frontend.reset-url.client}")
    private String resetPasswordUrlClient;

    @Value("${app.frontend.reset-url.manager}")
    private String resetPasswordUrlManager;

    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void envoyer(Validation validation) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(validation.getManager().getEmail());
            helper.setSubject("Votre code d'activation");

            String htmlContent = """
            <div style="font-family: Arial, sans-serif; background-color: #83BE40; padding: 30px;">
                <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                    <h2 style="text-align: center; color: #2c3e50;">Activation de compte</h2>
                    <p>Bonjour M/Mme/Mlle.<strong>%s</strong>,</p>
                    <p>Merci de vous être inscrit sur SMS-GATEWAY.</p>
                    <p>Voici votre code d'activation :</p>
                    <div style="text-align: center; font-size: 24px; font-weight: bold; margin: 20px 0; background: #eef; padding: 15px; border-radius: 5px;">%s</div>
                    <p>⏳ Ce code est valable pendant 60 minutes.</p>
                    <p style="color: #888; font-size: 12px; text-align: center;">
                        Si vous n'avez pas demandé cette inscription, veuillez ignorer ce message.
                    </p>
                    <p style="text-align: center; color: #aaa; margin-top: 20px;">— SMS-GATEWAY</p>
                </div>
            </div>
        """.formatted(validation.getManager().getNomManager(), validation.getCode());

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /** Envoie au client son ID, son email, son MDP temporaire */
    public void envoyerAccesClient(Client client, String rawPassword) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(client.getEmail());
            helper.setSubject("Vos accès SMS-GATEWAY");

            String html = """
            <div style="font-family: Arial, sans-serif; background-color: #83BE40; padding: 30px;">
              <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
                <h2 style="text-align: center; color: #2c3e50;">Bienvenue sur SMS-GATEWAY</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Votre compte a été créé par notre équipe.</p>

                <p><strong>Vos informations :</strong></p>
                <ul>
                  <li><strong>ID Client :</strong> %s</li>
                  <li><strong>Email :</strong> %s</li>
                  <li><strong>Mot de passe (temporaire) :</strong> %s</li>
                </ul>

                <p>⚠️ Par sécurité, veuillez changer votre mot de passe à la première connexion.</p>

                <p style="color: #888; font-size: 12px; text-align: center;">
                  Si vous n'êtes pas à l'origine de cette création, ignorez ce message.
                </p>
                <p style="text-align: center; color: #aaa; margin-top: 20px;">— SMS-GATEWAY</p>
              </div>
            </div>
            """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    client.getIdclients(),
                    client.getEmail(),
                    rawPassword
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            e.printStackTrace();
        }
    }

    public void envoyerResetClient(Client client, String token) {
        try {
            String link = resetPasswordUrlClient + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(client.getEmail());
            helper.setSubject("Réinitialisation de votre mot de passe");

            String html = """
                        <h2>Réinitialisation de mot de passe</h2>
                        <p>Bonjour %s,</p>
                        <p>Vous pouvez réinitialiser votre mot de passe en cliquant sur le lien ci-dessous :</p>
                        <p><a href="%s">%s</a></p>
                        <p>Ce lien est valable pendant une durée limitée.</p>
                    """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    link, link
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
    public void envoyerResetManager(Manager manager, String token) {
        try {
            String link = resetPasswordUrlManager + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(manager.getEmail());
            helper.setSubject("Réinitialisation de votre mot de passe Manager");

            String html = """
            <h2>Réinitialisation du mot de passe</h2>
            <p>Bonjour %s %s,</p>
            <p>Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe :</p>
            <p><a href="%s">%s</a></p>
            <p>⏳ Ce lien est valable pendant une durée limitée.</p>
        """.formatted(manager.getNomManager(), manager.getPrenomManager(), link, link);

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Value("${app.mail.from:noreply@solutech-one.com}")
    private String from; // centralise l'expéditeur
    public void envoyerFacture(Facture f, byte[] pdfBytes) {
        if (f.getClient() == null || f.getClient().getEmail() == null) {
            throw new IllegalStateException("Email client manquant pour l'envoi de facture " + f.getId());
        }

        String numero = "INV-" + f.getId().substring(0, 8).toUpperCase();
        String periode = f.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " - " + f.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String total = formatFcfa(f.getMontant()); // ex: 24 000

        String subject = "Votre facture " + numero + " — " + total + " FCFA";
        String html = """
        <div style="font-family: Arial, sans-serif; background:#f9f9f9; padding:24px">
          <div style="max-width:640px;margin:auto;background:#fff;border-radius:8px;padding:24px">
            <h2 style="margin:0 0 12px 0;color:#2c3e50">Facture %s</h2>
            <p style="margin:0 0 8px 0"><strong>Client :</strong> %s</p>
            <p style="margin:0 0 8px 0"><strong>Période :</strong> %s</p>
            <p style="margin:0 0 8px 0"><strong>Consommation :</strong> %d SMS</p>
            <p style="margin:0 0 8px 0"><strong>Total TTC :</strong> %s FCFA</p>
            <p>La facture PDF est jointe à ce message.</p>
            <p style="color:#999;font-size:12px;margin-top:16px">— SMS-GATEWAY</p>
          </div>
        </div>
        """.formatted(
                numero,
                nullToDash(f.getClient().getRaisonSociale()),
                periode,
                f.getConsommationSms(),
                total
        );

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(f.getClient().getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addAttachment(("Facture-" + numero + ".pdf").replace(' ', '_'),
                    new ByteArrayResource(pdfBytes));
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Envoi email facture échoué: " + e.getMessage(), e);
        }
    }

    private String nullToDash(String v) { return (v == null || v.isBlank()) ? "-" : v; }

    private String formatFcfa(BigDecimal v) {
        if (v == null) return "0";
        NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        return nf.format(v);
    }


}
