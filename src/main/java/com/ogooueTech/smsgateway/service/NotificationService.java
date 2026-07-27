package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Facture;
import com.ogooueTech.smsgateway.model.Manager;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:noreply@solutech-one.com}")
    private String from;

    @Value("${app.frontend.login-url.client:https://client.i-onsms.es/login}")
    private String loginUrlClient;

    @Value("${app.frontend.login-url.manager:https://backoffice.i-onsms.es/login}")
    private String loginUrlManager;

    @Value("${app.frontend.reset-url.client}")
    private String resetPasswordUrlClient;

    @Value("${app.frontend.reset-url.manager}")
    private String resetPasswordUrlManager;

    public NotificationService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /** Envoie les identifiants temporaires d'un manager. */
    public void envoyerIdentifiantsManager(Manager manager, String rawPassword) {
        String nomComplet = safe(manager.getNomManager()) + " " + safe(manager.getPrenomManager());
        String role = manager.getRole() != null ? manager.getRole().name() : "MANAGER";

        String details = infoRow("▣", "ID MANAGER", safe(manager.getIdManager()), null)
                + infoRow("○", "IDENTIFIANT", safe(manager.getEmail()), null)
                + infoRow("▤", "RÔLE", role, badge(role, "#111111", "#ffffff"))
                + passwordRow(rawPassword);

        String html = buildEmail(
                "Compte manager créé",
                "Bonjour " + nomComplet + ",",
                "Votre compte manager a été créé par notre équipe. Voici vos informations de connexion.",
                detailsSection(details),
                "Pour des raisons de sécurité, veuillez changer votre mot de passe dès votre première connexion.",
                "Accéder à mon espace",
                loginUrlManager
        );

        sendHtmlEmail(
                manager.getEmail(),
                "Votre compte manager SMS-GATEWAY",
                html
        );
    }

    /** Envoie au client son ID, son email, son mot de passe temporaire et son type de compte. */
    public void envoyerAccesClient(Client client, String rawPassword) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");
        String typeCompte = client.getTypeCompte() != null
                ? client.getTypeCompte().name()
                : "NON DÉFINI";

        String details = infoRow("▣", "ID CLIENT", safe(client.getIdclients()), null)
                + infoRow("○", "IDENTIFIANT", safe(client.getEmail()), null)
                + infoRow("▤", "TYPE DE COMPTE", typeCompte,
                badge(typeCompte, "#111111", "#ffffff"))
                + passwordRow(rawPassword);

        String html = buildEmail(
                "Compte créé",
                "Bonjour " + raisonSociale + ",",
                "Votre compte a été créé par notre équipe. Voici vos informations de connexion.",
                detailsSection(details),
                "Pour des raisons de sécurité, veuillez changer votre mot de passe dès votre première connexion.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmail(
                client.getEmail(),
                "Vos accès SMS-GATEWAY",
                html
        );
    }

    /** Envoie le lien de réinitialisation du mot de passe d'un client. */
    public void envoyerResetClient(Client client, String token) {
        String link = buildResetLink(resetPasswordUrlClient, token);
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String content = noticeBlock(
                "Ce lien est personnel et valable pendant une durée limitée. "
                        + "Ne le partagez avec personne."
        );

        String html = buildEmail(
                "Réinitialisation du mot de passe",
                "Bonjour " + raisonSociale + ",",
                "Une demande de réinitialisation de votre mot de passe a été effectuée. "
                        + "Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.",
                content,
                "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer ce message.",
                "Réinitialiser mon mot de passe",
                link
        );

        sendHtmlEmail(
                client.getEmail(),
                "Réinitialisation de votre mot de passe",
                html
        );
    }

    /** Envoie le lien de réinitialisation du mot de passe d'un manager. */
    public void envoyerResetManager(Manager manager, String token) {
        String link = buildResetLink(resetPasswordUrlManager, token);
        String nomComplet = safe(manager.getNomManager()) + " " + safe(manager.getPrenomManager());

        String content = noticeBlock(
                "Ce lien est personnel et valable pendant une durée limitée. "
                        + "Ne le partagez avec personne."
        );

        String html = buildEmail(
                "Réinitialisation du mot de passe",
                "Bonjour " + nomComplet + ",",
                "Une demande de réinitialisation de votre mot de passe manager a été effectuée. "
                        + "Cliquez sur le bouton ci-dessous pour choisir un nouveau mot de passe.",
                content,
                "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer ce message.",
                "Réinitialiser mon mot de passe",
                link
        );

        sendHtmlEmail(
                manager.getEmail(),
                "Réinitialisation de votre mot de passe Manager",
                html
        );
    }

    /** Envoie une facture PDF au client. */
    public void envoyerFacture(Facture facture, byte[] pdfBytes) {
        if (facture.getClient() == null || facture.getClient().getEmail() == null) {
            throw new IllegalStateException(
                    "Email client manquant pour l'envoi de facture " + facture.getId()
            );
        }

        String numero = "INV-" + facture.getId().substring(0, 8).toUpperCase(Locale.ROOT);
        String periode = facture.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " - "
                + facture.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String total = formatFcfa(facture.getMontant());
        String clientNom = safeOrDefault(facture.getClient().getRaisonSociale(), "Client");

        String details = infoRow("▣", "NUMÉRO DE FACTURE", numero, null)
                + infoRow("○", "CLIENT", clientNom, null)
                + infoRow("▤", "PÉRIODE", periode, null)
                + infoRow("#", "CONSOMMATION", facture.getConsommationSms() + " SMS", null)
                + infoRow("¤", "TOTAL TTC", total + " FCFA",
                badge(total + " FCFA", "#111111", "#ffffff"));

        String html = buildEmail(
                "Facture " + numero,
                "Bonjour " + clientNom + ",",
                "Votre facture est disponible. Le document PDF est joint à cet e-mail.",
                detailsSection(details),
                "Conservez cette facture pour votre suivi comptable.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmailWithAttachment(
                facture.getClient().getEmail(),
                "Votre facture " + numero + " — " + total + " FCFA",
                html,
                ("Facture-" + numero + ".pdf").replace(' ', '_'),
                pdfBytes
        );
    }

    /** Notifie un client de la suspension de son compte. */
    public void envoyerSuspensionClient(Client client) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String details = infoRow("▣", "ID CLIENT", safe(client.getIdclients()), null)
                + infoRow("!", "STATUT DU COMPTE", "SUSPENDU",
                badge("SUSPENDU", "#b42318", "#ffffff"));

        String html = buildEmail(
                "Compte suspendu",
                "Bonjour " + raisonSociale + ",",
                "Nous vous informons que votre compte SMS-GATEWAY a été suspendu.",
                detailsSection(details),
                "Vous ne pouvez plus envoyer de SMS ni accéder à votre espace client. "
                        + "Veuillez contacter notre support pour plus d'informations.",
                null,
                null
        );

        sendHtmlEmail(
                client.getEmail(),
                "Compte suspendu — SMS-GATEWAY",
                html
        );
    }

    /** Notifie un client de la réactivation de son compte. */
    public void envoyerReactivationClient(Client client) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String details = infoRow("▣", "ID CLIENT", safe(client.getIdclients()), null)
                + infoRow("✓", "STATUT DU COMPTE", "ACTIF",
                badge("ACTIF", "#067647", "#ffffff"));

        String html = buildEmail(
                "Compte réactivé",
                "Bonjour " + raisonSociale + ",",
                "Bonne nouvelle : votre compte SMS-GATEWAY a été réactivé.",
                detailsSection(details),
                "Vous pouvez désormais reprendre vos envois SMS et accéder à votre espace client.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmail(
                client.getEmail(),
                "Compte réactivé — SMS-GATEWAY",
                html
        );
    }

    /** Envoie la confirmation de création d'une demande de crédit. */
    public void envoyerDemandeCredit(Client client, int quantite, String requestCode) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String details = infoRow("#", "QUANTITÉ COMMANDÉE", quantite + " SMS", null)
                + infoRow("▣", "RÉFÉRENCE", safe(requestCode), null)
                + infoRow("…", "STATUT", "EN ATTENTE",
                badge("EN ATTENTE", "#d28a00", "#ffffff"));

        String html = buildEmail(
                "Commande de SMS enregistrée",
                "Bonjour " + raisonSociale + ",",
                "Votre commande de SMS a bien été enregistrée.",
                detailsSection(details),
                "Elle est en attente de validation. Vous serez notifié dès qu'elle sera approuvée ou rejetée.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmail(
                client.getEmail(),
                "Commande de SMS enregistrée — SMS-GATEWAY",
                html
        );
    }

    /** Envoie la confirmation d'approbation d'une demande de crédit. */
    public void envoyerCreditApprouve(Client client, int quantite, int nouveauSolde) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String details = infoRow("#", "QUANTITÉ APPROUVÉE", quantite + " SMS", null)
                + infoRow("▣", "NOUVEAU SOLDE", nouveauSolde + " SMS", null)
                + infoRow("✓", "STATUT", "APPROUVÉE",
                badge("APPROUVÉE", "#067647", "#ffffff"));

        String html = buildEmail(
                "Commande approuvée",
                "Bonjour " + raisonSociale + ",",
                "Votre commande de SMS a été approuvée et votre solde a été mis à jour.",
                detailsSection(details),
                "Merci pour votre confiance.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmail(
                client.getEmail(),
                "Commande de SMS approuvée — SMS-GATEWAY",
                html
        );
    }

    /** Envoie la notification de rejet d'une demande de crédit. */
    public void envoyerCreditRejete(Client client, int quantite, String raison) {
        String raisonSociale = safeOrDefault(client.getRaisonSociale(), "Client");

        String details = infoRow("#", "QUANTITÉ DEMANDÉE", quantite + " SMS", null)
                + infoRow("!", "MOTIF", safeOrDefault(raison, "Non précisé"), null)
                + infoRow("×", "STATUT", "REJETÉE",
                badge("REJETÉE", "#b42318", "#ffffff"));

        String html = buildEmail(
                "Commande rejetée",
                "Bonjour " + raisonSociale + ",",
                "Votre demande de crédit SMS n'a pas été approuvée.",
                detailsSection(details),
                "Vous pouvez contacter notre support pour obtenir plus d'informations.",
                "Accéder à mon espace",
                loginUrlClient
        );

        sendHtmlEmail(
                client.getEmail(),
                "Demande de crédit rejetée — SMS-GATEWAY",
                html
        );
    }

    /** Notifie un manager de la suspension de son compte. */
    public void envoyerSuspensionManager(Manager manager) {
        String nomComplet = safe(manager.getNomManager()) + " " + safe(manager.getPrenomManager());

        String details = infoRow("▣", "ID MANAGER", safe(manager.getIdManager()), null)
                + infoRow("!", "STATUT DU COMPTE", "SUSPENDU",
                badge("SUSPENDU", "#b42318", "#ffffff"));

        String html = buildEmail(
                "Compte manager suspendu",
                "Bonjour " + nomComplet + ",",
                "Votre compte manager SMS-GATEWAY a été suspendu.",
                detailsSection(details),
                "Vous n'avez plus accès à la plateforme. Veuillez contacter l'administrateur ou le support.",
                null,
                null
        );

        sendHtmlEmail(
                manager.getEmail(),
                "Compte Manager suspendu — SMS-GATEWAY",
                html
        );
    }

    /** Notifie un manager de la réactivation de son compte. */
    public void envoyerReactivationManager(Manager manager) {
        String nomComplet = safe(manager.getNomManager()) + " " + safe(manager.getPrenomManager());

        String details = infoRow("▣", "ID MANAGER", safe(manager.getIdManager()), null)
                + infoRow("✓", "STATUT DU COMPTE", "ACTIF",
                badge("ACTIF", "#067647", "#ffffff"));

        String html = buildEmail(
                "Compte manager réactivé",
                "Bonjour " + nomComplet + ",",
                "Bonne nouvelle : votre compte manager SMS-GATEWAY a été réactivé.",
                detailsSection(details),
                "Vous pouvez à nouveau accéder à la plateforme et gérer vos opérations.",
                "Accéder à mon espace",
                loginUrlManager
        );

        sendHtmlEmail(
                manager.getEmail(),
                "Compte Manager réactivé — SMS-GATEWAY",
                html
        );
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            javaMailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Échec de l'envoi de l'e-mail à " + to,
                    exception
            );
        }
    }

    private void sendHtmlEmailWithAttachment(
            String to,
            String subject,
            String html,
            String attachmentName,
            byte[] attachmentBytes
    ) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addAttachment(attachmentName, new ByteArrayResource(attachmentBytes));
            javaMailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Échec de l'envoi de l'e-mail avec pièce jointe à " + to,
                    exception
            );
        }
    }

    /**
     * Construit le gabarit commun de tous les e-mails.
     * Le HTML utilise principalement des tableaux et des styles inline
     * pour conserver un rendu stable dans Gmail, Outlook et les mobiles.
     */
    private String buildEmail(
            String title,
            String greeting,
            String intro,
            String contentHtml,
            String notice,
            String buttonLabel,
            String buttonUrl
    ) {
        String noticeHtml = isBlank(notice)
                ? ""
                : noticeBlock(notice);

        String buttonHtml = isBlank(buttonLabel) || isBlank(buttonUrl)
                ? ""
                : """
                <table role="presentation" border="0" cellpadding="0" cellspacing="0" style="margin-top:30px;">
                  <tr>
                    <td bgcolor="#111111" style="border-radius:7px;">
                      <a href="{{BUTTON_URL}}"
                         style="display:inline-block;padding:15px 28px;font-family:Arial,sans-serif;
                                font-size:14px;font-weight:700;color:#ffffff;text-decoration:none;
                                border-radius:7px;">
                        {{BUTTON_LABEL}}<span style="color:#e3a000;">.</span>
                      </a>
                    </td>
                  </tr>
                </table>
                """
                .replace("{{BUTTON_URL}}", escapeHtml(buttonUrl))
                .replace("{{BUTTON_LABEL}}", escapeHtml(buttonLabel));

        String template = """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>{{TITLE}}</title>
                </head>
                <body style="margin:0;padding:0;background:#f3f3f3;font-family:Arial,Helvetica,sans-serif;color:#111111;">
                  <table role="presentation" width="100%" border="0" cellpadding="0" cellspacing="0" style="background:#f3f3f3;">
                    <tr>
                      <td align="center" style="padding:28px 16px 36px 16px;">
                        <table role="presentation" width="560" border="0" cellpadding="0" cellspacing="0"
                               style="width:100%;max-width:560px;">
                          <tr>
                            <td style="padding:0 0 28px 0;font-size:15px;font-weight:800;color:#111111;">
                              SMS-GATEWAY
                            </td>
                          </tr>

                          <tr>
                            <td style="background:#ffffff;border:1px solid #e7e7e7;border-radius:14px;
                                       box-shadow:0 1px 2px rgba(0,0,0,0.04);padding:48px;">
                              <h1 style="margin:0 0 12px 0;font-size:24px;line-height:1.25;font-weight:500;color:#111111;">
                                {{TITLE}}
                              </h1>

                              <p style="margin:0;font-size:15px;line-height:1.65;color:#5f6368;">
                                {{GREETING}} {{INTRO}}
                              </p>

                              {{CONTENT}}
                              {{NOTICE}}
                              {{BUTTON}}
                            </td>
                          </tr>

                          <tr>
                            <td style="padding:26px 0 0 0;font-size:12px;line-height:1.9;color:#9aa0a6;">
                              Si vous n'êtes pas à l'origine de cette opération, vous pouvez ignorer ce message.<br>
                              SMS-GATEWAY<br>
                              Ceci est un e-mail automatique, merci de ne pas y répondre.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;

        return template
                .replace("{{TITLE}}", escapeHtml(title))
                .replace("{{GREETING}}", escapeHtml(greeting))
                .replace("{{INTRO}}", escapeHtml(intro))
                .replace("{{CONTENT}}", contentHtml == null ? "" : contentHtml)
                .replace("{{NOTICE}}", noticeHtml)
                .replace("{{BUTTON}}", buttonHtml);
    }


    /** Encadre les informations principales avec les séparateurs de la maquette. */
    private String detailsSection(String content) {
        return """
                <div style="margin-top:34px;padding:28px 0 6px 0;border-top:1px solid #ececec;
                            border-bottom:1px solid #ececec;">
                  {{CONTENT}}
                </div>
                """
                .replace("{{CONTENT}}", content == null ? "" : content);
    }

    /** Construit une ligne d'information compatible avec les clients e-mail. */
    private String infoRow(
            String icon,
            String label,
            String value,
            String customValueHtml
    ) {
        String renderedValue = customValueHtml != null
                ? customValueHtml
                : "<span style=\"font-size:15px;line-height:1.4;font-weight:700;color:#111111;\">"
                + escapeHtml(value)
                + "</span>";

        return """
                <table role="presentation" width="100%" border="0" cellpadding="0" cellspacing="0"
                       style="margin:0 0 20px 0;">
                  <tr>
                    <td width="30" valign="top" style="width:30px;padding-top:3px;font-size:16px;color:#a6abb1;">
                      {{ICON}}
                    </td>
                    <td valign="top">
                      <div style="margin:0 0 4px 0;font-size:12px;line-height:1.2;font-weight:700;
                                  letter-spacing:0.5px;color:#a0a5ab;">
                        {{LABEL}}
                      </div>
                      {{VALUE}}
                    </td>
                  </tr>
                </table>
                """
                .replace("{{ICON}}", escapeHtml(icon))
                .replace("{{LABEL}}", escapeHtml(label))
                .replace("{{VALUE}}", renderedValue);
    }

    /** Affiche le mot de passe temporaire dans un bloc distinct. */
    private String passwordRow(String rawPassword) {
        return """
                <div style="border-top:1px solid #ececec;margin-top:4px;padding-top:22px;">
                  <table role="presentation" width="100%" border="0" cellpadding="0" cellspacing="0">
                    <tr>
                      <td width="30" valign="top" style="width:30px;padding-top:3px;font-size:16px;color:#d99a00;">
                        &#128274;
                      </td>
                      <td valign="top">
                        <div style="margin:0 0 6px 0;font-size:12px;line-height:1.2;font-weight:700;
                                    letter-spacing:0.5px;color:#b47a00;">
                          MOT DE PASSE TEMPORAIRE
                        </div>
                        <span style="display:inline-block;padding:9px 14px;background:#fffaf0;border:1px solid #eadfca;
                                     border-radius:7px;font-size:16px;font-weight:700;letter-spacing:0.3px;color:#111111;">
                          {{PASSWORD}}
                        </span>
                      </td>
                    </tr>
                  </table>
                </div>
                """
                .replace("{{PASSWORD}}", escapeHtml(safe(rawPassword)));
    }

    /** Affiche un message d'information avec un accent doré, comme dans la maquette. */
    private String noticeBlock(String text) {
        return """
                <div style="margin-top:28px;padding:18px 20px;background:#fbfaf7;border-left:3px solid #e0a000;
                            border-radius:7px;font-size:13px;line-height:1.6;color:#513900;">
                  {{TEXT}}
                </div>
                """
                .replace("{{TEXT}}", escapeHtml(text));
    }

    /** Affiche une valeur sous forme de badge. */
    private String badge(String text, String background, String color) {
        return "<span style=\"display:inline-block;padding:5px 10px;border-radius:5px;"
                + "font-size:12px;font-weight:800;line-height:1;background:"
                + escapeHtml(background)
                + ";color:"
                + escapeHtml(color)
                + ";\">"
                + escapeHtml(text)
                + "</span>";
    }

    private String buildResetLink(String baseUrl, String token) {
        if (isBlank(baseUrl)) {
            throw new IllegalStateException("URL de réinitialisation non configurée");
        }
        if (isBlank(token)) {
            throw new IllegalArgumentException("Token de réinitialisation manquant");
        }

        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl
                + separator
                + "token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private String safeOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatFcfa(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRENCH);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(value);
    }
}