package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.SmsMuldesResponse;
import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.SmsMessage;
import com.ogooueTech.smsgateway.model.SmsRecipient;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.SmsMessageRepository;
import com.ogooueTech.smsgateway.repository.SmsRecipientRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SmsService {

    private final SmsMessageRepository smsRepo;
    private final SmsRecipientRepository recRepo;
    private final ClientRepository clientRepo; // injection par constructeur uniquement

    public SmsService(SmsMessageRepository smsRepo, SmsRecipientRepository recRepo, ClientRepository clientRepo) {
        this.smsRepo = smsRepo;
        this.recRepo = recRepo;
        this.clientRepo = clientRepo;
    }

    /** Valide la clé API et sa correspondance avec le clientId, renvoie l'entité Client si OK. */
    public Client assertApiKey(String apiKey, String clientId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Clé API requise");
        }
        Client client = clientRepo.findByCleApi(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Clé API invalide"));

        if (clientId != null && !client.getIdclients().equals(clientId)) {
            throw new IllegalArgumentException("Clé API non associée à ce client");
        }
        if (client.getStatutCompte() == StatutCompte.SUSPENDU) {
            throw new IllegalArgumentException("Compte suspendu. Aucune action n'est autorisée.");
        }

        return client;
    }

    public List<SmsMessage> getAllSmsEnvoyes() {
        return smsRepo.findByStatut(SmsStatus.ENVOYE);
    }


    // Générateur ref 6 chiffres
    private static final java.util.concurrent.atomic.AtomicLong LAST_REF =
            new java.util.concurrent.atomic.AtomicLong(900000);
    private String generateRef() { return String.format("%06d", LAST_REF.incrementAndGet()); }

    /* ---------- Création ---------- */

    // UNIDES
    public SmsMessage createUnides(String clientId, String emetteur, String destinataire, String corps) {
        validatePhone(destinataire);
        validateBody(corps);
        SmsMessage sms = new SmsMessage();
        sms.setRef(generateRef());
        sms.setType(SmsType.UNIDES);
        sms.setClientId(clientId);
        sms.setEmetteur(emetteur);
        sms.setDestinataire(normalizePhone(destinataire));
        sms.setCorps(corps);
        sms.setStatut(SmsStatus.EN_ATTENTE);
        sms.setNbDejaEnvoye(0);
        return smsRepo.save(sms);
    }

    // MULDES
    public SmsMessage createMuldes(String clientId, String emetteur, List<String> numeros, String corps) {
        validateBody(corps);
        if (numeros == null || numeros.isEmpty()) throw new IllegalArgumentException("Aucun destinataire");

        SmsMessage sms = baseMulti(clientId, emetteur, corps, SmsType.MULDES);

        // ✅ parent d'abord (insert + flush), puis enfants
        SmsMessage saved = smsRepo.saveAndFlush(sms);
        persistRecipients(saved, numeros);
        return saved;
    }

    // MULDESP
    public SmsMessage createMuldesp(String clientId, String emetteur, List<String> numeros, String corps,
                                    LocalDate dateDebut, Integer nbParJour,
                                    Integer intervalleMinutes, LocalDate dateFin) {
        validateBody(corps);
        if (dateDebut == null || dateFin == null || nbParJour == null || intervalleMinutes == null)
            throw new IllegalArgumentException("Paramètres de planification manquants");
        if (dateDebut.isAfter(dateFin)) throw new IllegalArgumentException("dateDebut > dateFin");

        SmsMessage sms = baseMulti(clientId, emetteur, corps, SmsType.MULDESP);
        sms.setDateDebutEnvoi(dateDebut);
        sms.setDateFinEnvoi(dateFin);
        sms.setNbParJour(nbParJour);
        sms.setIntervalleMinutes(intervalleMinutes);

        SmsMessage saved = smsRepo.saveAndFlush(sms);
        persistRecipients(saved, numeros);
        return saved;
    }


    private SmsMessage baseMulti(String clientId, String emetteur, String corps, SmsType type) {
        SmsMessage sms = new SmsMessage();
        sms.setRef(generateRef());
        sms.setType(type);
        sms.setClientId(clientId);
        sms.setEmetteur(emetteur);
        sms.setCorps(corps);
        sms.setStatut(SmsStatus.EN_ATTENTE);
        sms.setNbDejaEnvoye(0);
        return sms;
    }

    private void persistRecipients(SmsMessage sms, List<String> nums) {
        for (String n : nums) {
            String normalized = normalizePhone(n);
            validatePhone(normalized);
            SmsRecipient r = new SmsRecipient();
            r.setSms(sms);
            r.setNumero(normalized);
            r.setStatut(SmsStatus.EN_ATTENTE);
            recRepo.save(r);
        }
    }

    /* ---------- Envoi ---------- */

    public void envoyerImmediate(SmsMessage sms) {
        // ===== AJOUT: verrouillage client + règles PRE/POST-payé =====
        Client client = clientRepo.lockById(sms.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable: " + sms.getClientId()));

        int toSend;
        List<SmsRecipient> recs = List.of();

        if (sms.getType() == SmsType.UNIDES) {
            toSend = 1;
        } else if (sms.getType() == SmsType.MULDES || sms.getType() == SmsType.MULDESP) {
            recs = recRepo.findBySms_RefAndStatut(sms.getRef(), SmsStatus.EN_ATTENTE);
            toSend = recs.size();
            if (toSend == 0) {
                sms.setStatut(SmsStatus.ENVOYE);
                smsRepo.save(sms);
                return;
            }
        } else {
            // types inconnus => rien à faire
            return;
        }

        // Prépayé : débiter avant l’envoi (bloque si insuffisant)
        if (isPrepaye(client)) {
            debitPrepayeOuFail(client, toSend);
        }

        // Envoi effectif
        if (sms.getType() == SmsType.UNIDES) {
            envoyerUnitaire(sms.getDestinataire(), sms);
        } else {
            for (var r : recs) {
                envoyerUnitaire(r.getNumero(), sms, r);
            }
        }

        // Postpayé : incrémenter consommation réelle
        if (isPostpaye(client)) {
            creditPostpaye(client, toSend);
        }

        sms.setStatut(SmsStatus.ENVOYE);
        smsRepo.save(sms);
    }

    public void tickPlanification() {
        var planifies = smsRepo.findByTypeAndStatut(SmsType.MULDESP, SmsStatus.EN_ATTENTE);
        var now = LocalDateTime.now();
        var today = now.toLocalDate();

        for (SmsMessage sms : planifies) {
            // Vérifie si la date du jour est dans l’intervalle de planification
            if (sms.getDateDebutEnvoi() == null || sms.getDateFinEnvoi() == null) continue;
            if (today.isBefore(sms.getDateDebutEnvoi()) || today.isAfter(sms.getDateFinEnvoi())) {
                continue; // pas encore commencé ou déjà terminé
            }

            // Vérifie combien ont déjà été envoyés
            int deja = sms.getNbDejaEnvoye() == null ? 0 : sms.getNbDejaEnvoye();
            int quota = sms.getNbParJour() == null ? 1 : sms.getNbParJour();

            if (deja >= quota) {
                // Le quota du jour est atteint, on attend demain
                continue;
            }

            // Récupère les destinataires restants
            var pageRecipients = recRepo.findBySms_RefAndStatut(
                    sms.getRef(), SmsStatus.EN_ATTENTE, PageRequest.of(0, quota - deja)
            ).getContent();

            if (pageRecipients.isEmpty()) {
                sms.setStatut(SmsStatus.ENVOYE);
                smsRepo.save(sms);
                continue;
            }

            Client client = clientRepo.lockById(sms.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Client introuvable: " + sms.getClientId()));

            int toSend = pageRecipients.size();

            // Vérifie solde PREPAYE
            if (isPrepaye(client)) {
                int solde = getSoldeNet(client);
                if (solde <= 0) {
                    // pas de solde → on arrête pour aujourd’hui
                    continue;
                }
                if (solde < toSend) {
                    toSend = solde;
                    pageRecipients = pageRecipients.subList(0, toSend);
                }
                debitPrepayeOuFail(client, toSend);
            }

            // Envoi des SMS restants
            for (var r : pageRecipients) {
                envoyerUnitaire(r.getNumero(), sms, r);
            }

            // Postpayé : incrémente consommation
            if (isPostpaye(client)) {
                creditPostpaye(client, toSend);
            }

            // Mets à jour le compteur
            sms.setNbDejaEnvoye(deja + toSend);

            // Vérifie si tout est envoyé
            boolean reste = recRepo.existsBySms_RefAndStatut(sms.getRef(), SmsStatus.EN_ATTENTE);
            if (!reste && today.isAfter(sms.getDateFinEnvoi())) {
                sms.setStatut(SmsStatus.ENVOYE);
            }

            smsRepo.save(sms);
        }
    }


    /* ---------- Mock passerelle ---------- */
    private void envoyerUnitaire(String numero, SmsMessage sms) {
        int current = sms.getNbDejaEnvoye() == null ? 0 : sms.getNbDejaEnvoye();
        sms.setNbDejaEnvoye(current + 1);
    }

    private void envoyerUnitaire(String numero, SmsMessage sms, SmsRecipient r) {
        r.setStatut(SmsStatus.ENVOYE);
        recRepo.save(r);
        int current = sms.getNbDejaEnvoye() == null ? 0 : sms.getNbDejaEnvoye();
        sms.setNbDejaEnvoye(current + 1);
    }

    /* ---------- Helpers ---------- */
    private String normalizePhone(String number) {
        return number == null ? "" : number.trim().replaceAll("[\\s\\-\\.()]", "");
    }
    private void validatePhone(String number) {
        if (number == null || !number.matches("^\\+?\\d{8,15}$"))
            throw new IllegalArgumentException("Numéro invalide");
    }
    private void validateBody(String body) {
        if (body == null || body.isBlank() || body.length() > 160)
            throw new IllegalArgumentException("Corps de SMS invalide (1..160)");
    }

    // Récupérer les SMS envoyés à un utilisateur (par numéro)
    public List<SmsMessage> getSmsByDestinataire(String numero) {
        return smsRepo.findByDestinataire(numero);
    }

    // Récupérer uniquement les SMS déjà envoyés (statut = ENVOYE)
    public List<SmsMessage> getSmsEnvoyesByDestinataire(String numero) {
        return smsRepo.findByDestinataireAndStatut(numero, SmsStatus.ENVOYE);
    }

    public Page<SmsMessage> getUnidesByClient(String clientId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return smsRepo.findByClientIdAndType(clientId, SmsType.UNIDES, pageable);
    }

    public Page<SmsMessage> getUnidesByClientAndStatut(String clientId, SmsStatus statut, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return smsRepo.findByClientIdAndTypeAndStatut(clientId, SmsType.UNIDES, statut, pageable);
    }

    public Page<SmsMessage> getUnidesByClientBetweenDates(
            String clientId, LocalDateTime start, LocalDateTime end, SmsStatus statut,
            Integer page, Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (statut == null) {
            return smsRepo.findByClientIdAndTypeBetweenDates(clientId, SmsType.UNIDES, start, end, pageable);
        }
        return smsRepo.findByClientIdAndTypeAndStatutBetweenDates(clientId, SmsType.UNIDES, statut, start, end, pageable);
    }

    public Page<SmsMessage> getMuldesByClient(String clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return smsRepo.findByClientIdAndType(clientId, SmsType.MULDES, pageable);
    }

    public Page<SmsMessage> getMuldesByClientAndStatut(String clientId, SmsStatus statut, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return smsRepo.findByClientIdAndTypeAndStatut(clientId, SmsType.MULDES, statut, pageable);
    }

    /** Parse une chaîne de numéros "destinataire" en liste. */
    public static List<String> parseDestinataires(String raw) {
        if (raw == null || raw.isBlank()) return List.of();

        String s = raw.trim();

        if ((s.startsWith("[") && s.endsWith("]"))) {
            s = s.substring(1, s.length() - 1);
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .map(x -> x.replaceAll("^\"|\"$", ""))
                    .filter(x -> !x.isBlank())
                    .map(SmsService::normaliserNumero)
                    .distinct()
                    .collect(Collectors.toList());
        }

        String[] parts = s.split("[,;|\\s]+");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .map(SmsService::normaliserNumero)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Normalise un numéro (ex: retire + et espaces, préfixe 241 si format court) */
    public static String normaliserNumero(String n) {
        String s = n.replace(" ", "").replace("+", "");
        if (s.length() <= 9 && !s.startsWith("241")) s = "241" + s;
        return s;
    }

    public Page<SmsMuldesResponse> getMuldesWithParsedRecipients(
            String clientId, Integer page, Integer size, SmsStatus statut) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<SmsMessage> messages = (statut == null)
                ? smsRepo.findByClientIdAndType(clientId, SmsType.MULDES, pageable)
                : smsRepo.findByClientIdAndTypeAndStatut(clientId, SmsType.MULDES, statut, pageable);

        // refs de la page courante
        List<String> refs = messages.getContent().stream()
                .map(SmsMessage::getRef)
                .toList();

        // charge tous les destinataires en une fois
        var allRecipients = recRepo.findBySms_RefIn(refs);

        // ref -> [numéros]
        var recipientsByRef = allRecipients.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSms().getRef(),
                        Collectors.mapping(SmsRecipient::getNumero, Collectors.toList())
                ));

        return messages.map(m -> new SmsMuldesResponse(
                m.getRef(),
                m.getClientId(),
                m.getEmetteur(),
                m.getCorps(),
                m.getType(),
                m.getStatut(),
                m.getCreatedAt(),
                recipientsByRef.getOrDefault(
                        m.getRef(),
                        parseDestinataires(m.getDestinataire())
                )
        ));
    }

    /* ===================== AJOUT: helpers PRE/POST-payé ===================== */

    private boolean isPrepaye(Client c) {
        return "PREPAYE".equalsIgnoreCase(String.valueOf(getTypeCompte(c)));
    }
    private boolean isPostpaye(Client c) {
        return "POSTPAYE".equalsIgnoreCase(String.valueOf(getTypeCompte(c)));
    }

    private void debitPrepayeOuFail(Client client, int nbSms) {
        int solde = getSoldeNet(client);
        if (solde <= 0)
            throw new IllegalStateException("Solde insuffisant : votre compte prépayé est à 0. Veuillez recharger.");
        if (solde < nbSms)
            throw new IllegalStateException("Solde insuffisant : " + solde + " SMS restants, " + nbSms + " requis.");
        setSoldeNet(client, solde - nbSms);
        clientRepo.save(client);
    }

    private void creditPostpaye(Client client, int nbSms) {
        int solde = getSoldeNet(client);
        setSoldeNet(client, solde + nbSms);
        clientRepo.save(client);
    }

    // Ces trois helpers reflètent ceux utilisés dans CreditService pour s'adapter à ta classe Client
    private Object getTypeCompte(Client c) {
        try {
            return c.getClass().getMethod("getTypeCompte").invoke(c);
        } catch (Exception ignore) {
            try {
                return c.getClass().getMethod("getType_compte").invoke(c);
            } catch (Exception e) {
                throw new IllegalStateException("Client: getter typeCompte/type_compte introuvable");
            }
        }
    }
    private int getSoldeNet(Client c) {
        try {
            Object v = c.getClass().getMethod("getSoldeNet").invoke(c);
            return v == null ? 0 : ((Number) v).intValue();
        } catch (Exception ignore) {
            try {
                Object v = c.getClass().getMethod("getSolde_net").invoke(c);
                return v == null ? 0 : ((Number) v).intValue();
            } catch (Exception e) {
                throw new IllegalStateException("Client: getter soldeNet/solde_net introuvable");
            }
        }
    }
    private void setSoldeNet(Client c, int value) {
        try {
            c.getClass().getMethod("setSoldeNet", Integer.class).invoke(c, value);
        } catch (Exception ignore) {
            try {
                c.getClass().getMethod("setSolde_net", Integer.class).invoke(c, value);
            } catch (Exception e) {
                throw new IllegalStateException("Client: setter soldeNet/solde_net introuvable");
            }
        }
    }
}
