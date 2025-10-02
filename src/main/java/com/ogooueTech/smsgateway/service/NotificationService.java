package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Manager;
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

    public void envoyerIdentifiantsManager(Manager manager, String rawPassword) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(manager.getEmail());
            helper.setSubject("Vos identifiants SMS-GATEWAY");

            String html = """
        <div style="font-family: Arial, sans-serif; background-color: #83BE40; padding: 30px;">
          <div style="max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1);">
            <h2 style="text-align: center; color: #2c3e50;">Bienvenue sur SMS-GATEWAY</h2>
            <p>Bonjour <strong>%s %s</strong>,</p>
            <p>Votre compte manager a été créé avec succès.</p>

            <p><strong>Vos informations :</strong></p>
            <ul>
              <li><strong>ID Manager :</strong> %s</li>
              <li><strong>Identifiant (email) :</strong> %s</li>
              <li><strong>Mot de passe (temporaire) :</strong> %s</li>
            </ul>

            <p style="color: red;">⚠️ Veuillez modifier ce mot de passe lors de votre première connexion.</p>

            <p style="color: #888; font-size: 12px; text-align: center;">
              Si vous n'êtes pas à l'origine de cette création, ignorez ce message.
            </p>
            <p style="text-align: center; color: #aaa; margin-top: 20px;">— SMS-GATEWAY</p>
          </div>
        </div>
        """.formatted(
                    manager.getNomManager(),
                    manager.getPrenomManager(),
                    manager.getIdManager(),
                    manager.getEmail(),
                    rawPassword
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            e.printStackTrace();
        }
    }




    /** Envoie au client son ID, son email, son MDP temporaire et son type de compte */
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
              <li><strong>Identifiant (email) :</strong> %s</li>
              <li><strong>Mot de passe (temporaire) :</strong> %s</li>
              <li><strong>Type de compte :</strong> %s</li>
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
                    rawPassword,
                    client.getTypeCompte() != null ? client.getTypeCompte().name() : "Non défini"
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
    /** Notifie un client de la suspension de son compte */
    public void envoyerSuspensionClient(Client client) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(client.getEmail());
            helper.setSubject("⚠️ Compte suspendu — SMS-GATEWAY");

            String html = """
        <div style="font-family: Arial, sans-serif; background-color: #ffecec; padding: 30px;">
          <div style="max-width: 600px; margin: auto; background: white; padding: 25px; border-radius: 8px; border: 1px solid #e74c3c;">
            <h2 style="color:#e74c3c; text-align:center;">Votre compte a été suspendu</h2>
            <p>Bonjour <strong>%s</strong>,</p>
            <p>Nous vous informons que votre compte <strong>ID : %s</strong> a été <span style="color:#e74c3c;">suspendu</span>.</p>
            <p>Vous ne pouvez plus ni envoyé de sms ni accéder à votre espace client.</p>
            <p>👉 Veuillez contacter notre support pour plus d’informations.</p>
            <p style="text-align:center; color:#999; font-size:12px; margin-top:20px;">— SMS-GATEWAY</p>
          </div>
        </div>
        """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    client.getIdclients()
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /** Notifie un client de la réactivation de son compte */
    public void envoyerReactivationClient(Client client) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("noreply@solutech-one.com");
            helper.setTo(client.getEmail());
            helper.setSubject("✅ Compte réactivé — SMS-GATEWAY");

            String html = """
        <div style="font-family: Arial, sans-serif; background-color: #e6f9ec; padding: 30px;">
          <div style="max-width: 600px; margin: auto; background: white; padding: 25px; border-radius: 8px; border: 1px solid #27ae60;">
            <h2 style="color:#27ae60; text-align:center;">Votre compte a été réactivé</h2>
            <p>Bonjour <strong>%s</strong>,</p>
            <p>Bonne nouvelle 🎉 ! Votre compte <strong>ID : %s</strong> a été <span style="color:#27ae60;">réactivé</span>.</p>
            <p>Vous pouvez désormais reprendre vos envois SMS et accéder à votre espace client.</p>
            <p>Merci de votre confiance.</p>
            <p style="text-align:center; color:#999; font-size:12px; margin-top:20px;">— SMS-GATEWAY</p>
          </div>
        </div>
        """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    client.getIdclients()
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    /** 🔔 Mail : création d'une demande de crédit */
    public void envoyerDemandeCredit(Client client, int quantite, String requestCode) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(client.getEmail());
            helper.setSubject("Demande de crédit enregistrée — SMS-GATEWAY");

            String html = """
        <div style="font-family: Arial, sans-serif; background:#f9f9f9; padding:24px">
          <div style="max-width:600px; margin:auto; background:#fff; border-radius:8px; padding:24px">
            <h2 style="color:#2c3e50">Demande de crédit reçue ✅</h2>
            <p>Bonjour <strong>%s</strong>,</p>
            <p>Votre demande de crédit de <strong>%d SMS</strong> a bien été enregistrée.</p>
            <p><b>Request Code :</b> <code style="background:#eee; padding:2px 6px; border-radius:4px">%s</code></p>
            <p>👉 Elle est en attente de validation. Vous serez notifié dès qu’elle sera validée ou rejetée.</p>
            <p style="color:#999; font-size:12px; margin-top:16px">— SMS-GATEWAY</p>
          </div>
        </div>
        """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    quantite,
                    requestCode
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


    /** 🔔 Mail : approbation d'une demande de crédit */
    public void envoyerCreditApprouve(Client client, int quantite, int nouveauSolde) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(client.getEmail());
            helper.setSubject("Crédit validé — SMS-GATEWAY");

            String html = """
            <div style="font-family: Arial, sans-serif; background:#e6f9ec; padding:24px">
              <div style="max-width:600px; margin:auto; background:#fff; border-radius:8px; padding:24px">
                <h2 style="color:#27ae60">Crédit validé ✅</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Votre demande de <strong>%d SMS</strong> a été approuvée.</p>
                <p>📌 Nouveau solde : <strong>%d SMS</strong></p>
                <p>Merci pour votre confiance.</p>
                <p style="color:#999; font-size:12px; margin-top:16px">— SMS-GATEWAY</p>
              </div>
            </div>
            """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    quantite,
                    nouveauSolde
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /** 🔔 Mail : rejet d'une demande de crédit */
    public void envoyerCreditRejete(Client client, int quantite, String raison) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(client.getEmail());
            helper.setSubject("Demande de crédit rejetée — SMS-GATEWAY");

            String html = """
            <div style="font-family: Arial, sans-serif; background:#ffecec; padding:24px">
              <div style="max-width:600px; margin:auto; background:#fff; border-radius:8px; padding:24px; border:1px solid #e74c3c">
                <h2 style="color:#e74c3c">Demande rejetée ❌</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>Votre demande de <strong>%d SMS</strong> a été rejetée.</p>
                <p>Motif : <em>%s</em></p>
                <p>👉 Vous pouvez contacter notre support si besoin.</p>
                <p style="color:#999; font-size:12px; margin-top:16px">— SMS-GATEWAY</p>
              </div>
            </div>
            """.formatted(
                    client.getRaisonSociale() != null ? client.getRaisonSociale() : "Client",
                    quantite,
                    raison
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /** 🔔 Mail : suspension d’un Manager */
    public void envoyerSuspensionManager(Manager manager) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(manager.getEmail());
            helper.setSubject("⚠️ Compte Manager suspendu — SMS-GATEWAY");

            String html = """
            <div style="font-family: Arial, sans-serif; background-color:#ffecec; padding:30px">
              <div style="max-width:600px;margin:auto;background:#fff;padding:25px;border-radius:8px;border:1px solid #e74c3c">
                <h2 style="color:#e74c3c; text-align:center;">Compte Manager suspendu</h2>
                <p>Bonjour <strong>%s %s</strong>,</p>
                <p>Votre compte <strong>ID : %s</strong> a été <span style="color:#e74c3c;">suspendu</span>.</p>
                <p>Vous n’avez plus accès à la plateforme jusqu’à résolution du problème.</p>
                <p>👉 Veuillez contacter l’administrateur ou le support pour plus d’informations.</p>
                <p style="text-align:center;color:#999;font-size:12px;margin-top:20px">— SMS-GATEWAY</p>
              </div>
            </div>
            """.formatted(
                    manager.getNomManager(),
                    manager.getPrenomManager(),
                    manager.getIdManager()
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    /** 🔔 Mail : réactivation d’un Manager */
    public void envoyerReactivationManager(Manager manager) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(manager.getEmail());
            helper.setSubject("✅ Compte Manager réactivé — SMS-GATEWAY");

            String html = """
            <div style="font-family: Arial, sans-serif; background-color:#e6f9ec; padding:30px">
              <div style="max-width:600px;margin:auto;background:#fff;padding:25px;border-radius:8px;border:1px solid #27ae60">
                <h2 style="color:#27ae60; text-align:center;">Compte Manager réactivé</h2>
                <p>Bonjour <strong>%s %s</strong>,</p>
                <p>Bonne nouvelle 🎉 ! Votre compte <strong>ID : %s</strong> a été <span style="color:#27ae60;">réactivé</span>.</p>
                <p>Vous pouvez à nouveau accéder à la plateforme et gérer vos opérations.</p>
                <p>Merci de votre collaboration.</p>
                <p style="text-align:center;color:#999;font-size:12px;margin-top:20px">— SMS-GATEWAY</p>
              </div>
            </div>
            """.formatted(
                    manager.getNomManager(),
                    manager.getPrenomManager(),
                    manager.getIdManager()
            );

            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }


}
