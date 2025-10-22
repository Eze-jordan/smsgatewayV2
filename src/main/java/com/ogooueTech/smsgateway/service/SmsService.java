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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional
public class SmsService {
    private static final AtomicInteger SEQUENCE = new AtomicInteger(new SecureRandom().nextInt(1000));
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SmsMessageRepository smsRepo;
    private final SmsRecipientRepository recRepo;
    private final ClientRepository clientRepo; // injection par constructeur uniquement

    public SmsService(SmsMessageRepository smsRepo, SmsRecipientRepository recRepo, ClientRepository clientRepo) {
        this.smsRepo = smsRepo;
        this.recRepo = recRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * ✅ Version simplifiée pour les intégrations externes :
     * Vérifie uniquement la clé API et retourne le client associé.
     */
    public Client assertApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Clé API requise");
        }

        Client client = clientRepo.findByCleApi(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("Clé API invalide"));

        if (client.getStatutCompte() == StatutCompte.SUSPENDU) {
            throw new IllegalArgumentException("Compte suspendu. Aucune action n'est autorisée.");
        }

        // Vérifie si la clé est expirée
        if (client.getCleApiExpiration() == null || client.getCleApiExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Clé API expirée. Veuillez la régénérer depuis votre espace client.");
        }

        return client;
    }



    public List<SmsMessage> getAllSmsEnvoyes() {
        return smsRepo.findByStatut(SmsStatus.ENVOYE);
    }




    /**
     * Génère une référence courte, robuste et unique (6 à 8 caractères).
     * Combinaison du temps, d’un compteur, et d’un peu d’aléatoire.
     * Exemple : "A93724" ou "H12893"
     */
    private String generateRef() {
        long timePart = Instant.now().toEpochMilli() % 100000; // 5 derniers chiffres du timestamp
        int seqPart = SEQUENCE.getAndIncrement() % 1000;       // incrément cyclique
        int randomPart = RANDOM.nextInt(90) + 10;              // deux chiffres aléatoires (10–99)
        long combined = (timePart + seqPart + randomPart) % 999999; // borne à 6 chiffres
        return String.format("%06d", combined);
    }
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
        sms.setDestinataire(normaliserNumero(destinataire));
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
            String normalized = normaliserNumero(n);
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
        // 🔒 Ignore si déjà traité
        if (sms.getStatut() == SmsStatus.ENVOYE) return;

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
                // Aucun destinataire restant → rien à faire
                return;
            }
        } else {
            return;
        }

        // Envoi effectif (on marque en attente pour le cron)
        if (sms.getType() == SmsType.UNIDES) {
            envoyerUnitaire(sms.getDestinataire(), sms);
        } else {
            for (var r : recs) {
                envoyerUnitaire(r.getNumero(), sms, r);
            }
        }

        // ✅ Ne change PAS le compteur ici (incrément se fera dans markAsSent)
        // ✅ Laisse le statut EN_ATTENTE pour la passerelle externe
        sms.setStatut(SmsStatus.EN_ATTENTE);
        smsRepo.save(sms);
    }


        /**
         * Récupère tous les SMS en attente avec leurs destinataires complets
         * Version optimisée en une seule requête
         */
        public List<PendingSmsDetails> getPendingSmsOptimized() {
            LocalDate today = LocalDate.now();

            // 1. Récupérer tous les SMS en attente
            List<SmsMessage> allPending = smsRepo.findByStatut(SmsStatus.EN_ATTENTE);

            // 2. Filtrer selon les règles métier
            List<SmsMessage> filteredSms = allPending.stream()
                    .filter(sms -> shouldIncludeInPending(sms, today))
                    .collect(Collectors.toList());

            if (filteredSms.isEmpty()) {
                return List.of();
            }

            // 3. Récupérer tous les destinataires en une seule requête
            List<String> smsRefs = filteredSms.stream()
                    .map(SmsMessage::getRef)
                    .collect(Collectors.toList());

            // Récupère tous les SmsRecipient pour ces SMS
            List<SmsRecipient> allRecipients = recRepo.findBySms_RefIn(smsRefs);

            // Groupe par référence SMS et filtre seulement ceux en statut EN_ATTENTE
            Map<String, List<String>> recipientsBySmsRef = allRecipients.stream()
                    .filter(recipient -> recipient.getStatut() == SmsStatus.EN_ATTENTE)
                    .collect(Collectors.groupingBy(
                            recipient -> recipient.getSms().getRef(),
                            Collectors.mapping(SmsRecipient::getNumero, Collectors.toList())
                    ));

            // 4. Construire le résultat final
            return filteredSms.stream()
                    .map(sms -> {
                        PendingSmsDetails details = new PendingSmsDetails();
                        details.setSms(sms);

                        List<String> recipients;
                        if (sms.getType() == SmsType.UNIDES) {
                            // Pour UNIDES, utiliser le champ destinataire direct
                            recipients = sms.getDestinataire() != null ?
                                    List.of(sms.getDestinataire()) :
                                    List.of();
                        } else {
                            // Pour MULDES et MULDESP, utiliser la table des destinataires
                            recipients = recipientsBySmsRef.getOrDefault(sms.getRef(), List.of());
                        }

                        details.setRecipients(recipients);
                        details.setTotalRecipients(recipients.size());
                        return details;
                    })
                    .collect(Collectors.toList());
        }

        /**
         * Détermine si un SMS doit être inclus dans la liste des en attente
         */
        private boolean shouldIncludeInPending(SmsMessage sms, LocalDate today) {
            // Pour les SMS UNIDES et MULDES standard, toujours inclus s'ils ont des destinataires
            if (sms.getType() == SmsType.UNIDES) {
                return sms.getDestinataire() != null && !sms.getDestinataire().isBlank();
            }

            if (sms.getType() == SmsType.MULDES) {
                // Vérifier s'il reste des destinataires en attente pour ce SMS
                return recRepo.existsBySms_RefAndStatut(sms.getRef(), SmsStatus.EN_ATTENTE);
            }

            // Pour les SMS MULDESP (planifiés), vérifier la date et le quota
            if (sms.getType() == SmsType.MULDESP) {
                // Vérifier si nous sommes dans la période de planification
                if (sms.getDateDebutEnvoi() != null && sms.getDateFinEnvoi() != null) {
                    boolean isWithinDateRange = !today.isBefore(sms.getDateDebutEnvoi()) &&
                            !today.isAfter(sms.getDateFinEnvoi());

                    if (!isWithinDateRange) {
                        return false; // En dehors de la période de planification
                    }

                    // Vérifier si le quota du jour n'est pas atteint
                    int dejaEnvoye = sms.getNbDejaEnvoye() != null ? sms.getNbDejaEnvoye() : 0;
                    int quotaJournalier = sms.getNbParJour() != null ? sms.getNbParJour() : Integer.MAX_VALUE;

                    // Vérifier s'il reste des destinataires en attente
                    boolean hasPendingRecipients = recRepo.existsBySms_RefAndStatut(sms.getRef(), SmsStatus.EN_ATTENTE);

                    return dejaEnvoye < quotaJournalier && hasPendingRecipients;
                }
            }

            return false;
        }

        /**
         * Classe interne pour retourner les détails complets des SMS en attente
         */
        public static class PendingSmsDetails {
            private SmsMessage sms;
            private List<String> recipients;
            private int totalRecipients;

            // Constructeurs
            public PendingSmsDetails() {}

            public PendingSmsDetails(SmsMessage sms, List<String> recipients) {
                this.sms = sms;
                this.recipients = recipients;
                this.totalRecipients = recipients != null ? recipients.size() : 0;
            }

            // Getters et setters
            public SmsMessage getSms() { return sms; }
            public void setSms(SmsMessage sms) { this.sms = sms; }

            public List<String> getRecipients() { return recipients; }
            public void setRecipients(List<String> recipients) {
                this.recipients = recipients;
                this.totalRecipients = recipients != null ? recipients.size() : 0;
            }

            public int getTotalRecipients() { return totalRecipients; }
            public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }

            @Override
            public String toString() {
                return String.format("PendingSmsDetails{ref=%s, type=%s, recipients=%d}",
                        sms != null ? sms.getRef() : "null",
                        sms != null ? sms.getType() : "null",
                        totalRecipients);
            }
        }

        // ... le reste de votre code existant ...


    @Transactional
    public void markAsSent(String ref) {
        SmsMessage sms = smsRepo.findByRef(ref)
                .orElseThrow(() -> new IllegalArgumentException("SMS introuvable avec ref: " + ref));

        // 🔒 Ignore si déjà envoyé
        if (sms.getStatut() == SmsStatus.ENVOYE) return;

        Client client = clientRepo.lockById(sms.getClientId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable: " + sms.getClientId()));

        List<SmsRecipient> recs = recRepo.findBySms_RefAndStatut(ref, SmsStatus.EN_ATTENTE);
        int toDebit = recs.size();
        if (toDebit <= 0) return;

        if (isPrepaye(client)) {
            debitPrepayeOuFail(client, toDebit);
        }

        // ✅ Incrément uniquement ici
        sms.setNbDejaEnvoye((sms.getNbDejaEnvoye() == null ? 0 : sms.getNbDejaEnvoye()) + toDebit);
        sms.setStatut(SmsStatus.ENVOYE);
        smsRepo.save(sms);

        // ✅ Marque les destinataires comme envoyés
        for (SmsRecipient r : recs) {
            r.setStatut(SmsStatus.ENVOYE);
            recRepo.save(r);
        }

        if (isPostpaye(client)) {
            creditPostpaye(client, toDebit);
        }
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
                sms.setStatut(SmsStatus.EN_ATTENTE);
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
           //     debitPrepayeOuFail(client, toSend);
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
         //       sms.setStatut(SmsStatus.ENVOYE);
            }

            smsRepo.save(sms);
        }
    }


    private void envoyerUnitaire(String numero, SmsMessage sms) {
        // Pour un SMS unitaire (UNIDES)
        SmsRecipient r = new SmsRecipient();
        r.setSms(sms);
        r.setNumero(numero);
        r.setStatut(SmsStatus.EN_ATTENTE);
        recRepo.save(r);
    }


    private void envoyerUnitaire(String numero, SmsMessage sms, SmsRecipient r) {
        r.setStatut(SmsStatus.EN_ATTENTE);
        recRepo.save(r);
    }

    /* ---------- Helpers ---------- */
    private void validatePhone(String number) {
        if (number == null || number.isBlank()) {
            throw new IllegalArgumentException("Numéro invalide: ne peut pas être vide");
        }

        // Vérifie la longueur (6-24 caractères)
        if (number.length() < 6 || number.length() > 24) {
            throw new IllegalArgumentException(
                    "Numéro invalide: doit contenir entre 6 et 24 caractères. Longueur actuelle: " + number.length()
            );
        }

        // Vérifie le format: commence par + optionnel, suivi uniquement de chiffres
        if (!number.matches("^\\+?\\d+$")) {
            throw new IllegalArgumentException(
                    "Numéro invalide: doit contenir uniquement des chiffres et optionnellement un + au début"
            );
        }

        // Vérifie qu'après le + il y a au moins 6 chiffres
        String chiffresSeuls = number.replace("+", "");
        if (chiffresSeuls.length() < 6) {
            throw new IllegalArgumentException(
                    "Numéro invalide: doit contenir au moins 6 chiffres. Chiffres actuels: " + chiffresSeuls.length()
            );
        }
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
        if (n == null || n.isBlank()) {
            return n;
        }

        // Garde seulement les chiffres et le + (en début de numéro)
        String s = n.replaceAll("\\s", ""); // Supprime seulement les espaces

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

    // Récupérer tous les UNIDES (simples) pour un client
    public List<SmsMessage> getAllUnides(String clientId) {
        return smsRepo.findByClientIdAndType(clientId, SmsType.UNIDES, Pageable.unpaged())
                .getContent();
    }

    // Récupérer tous les MULDES avec les destinataires associés
    public List<SmsMuldesResponse> getAllMuldes(String clientId) {
        List<SmsMessage> messages = smsRepo.findByClientIdAndType(clientId, SmsType.MULDES, Pageable.unpaged())
                .getContent();

        List<String> refs = messages.stream().map(SmsMessage::getRef).toList();
        var allRecipients = recRepo.findBySms_RefIn(refs);

        var recipientsByRef = allRecipients.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSms().getRef(),
                        Collectors.mapping(SmsRecipient::getNumero, Collectors.toList())
                ));

        return messages.stream()
                .map(m -> new SmsMuldesResponse(
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
                ))
                .toList();
    }

    // Récupérer tous les MULDESP (planifiés) avec les destinataires associés
    public List<SmsMuldesResponse> getAllMuldesp(String clientId) {
        List<SmsMessage> messages = smsRepo.findByClientIdAndType(clientId, SmsType.MULDESP, Pageable.unpaged())
                .getContent();

        List<String> refs = messages.stream().map(SmsMessage::getRef).toList();
        var allRecipients = recRepo.findBySms_RefIn(refs);

        var recipientsByRef = allRecipients.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getSms().getRef(),
                        Collectors.mapping(SmsRecipient::getNumero, Collectors.toList())
                ));

        return messages.stream()
                .map(m -> new SmsMuldesResponse(
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
                ))
                .toList();
    }
    /**
     * Supprime un SMS spécifique d'un client par sa référence
     */
    @Transactional
    public void deleteClientSmsByRef(String clientId, String ref) {
        SmsMessage sms = smsRepo.findByRefAndClientId(ref, clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "SMS avec référence " + ref + " introuvable pour le client " + clientId));

        // Supprimer d'abord tous les destinataires associés
        recRepo.deleteBySms_Ref(ref);

        // Puis supprimer le SMS
        smsRepo.delete(sms);
    }

    /**
     * Supprime tous les SMS d'un client (tous types confondus)
     */
    @Transactional
    public void deleteAllSmsByClient(String clientId) {
        // Récupérer tous les SMS du client (tous types)
        List<SmsMessage> clientSms = smsRepo.findByClientId(clientId);

        if (!clientSms.isEmpty()) {
            // Récupérer toutes les références
            List<String> refs = clientSms.stream()
                    .map(SmsMessage::getRef)
                    .collect(Collectors.toList());

            // Supprimer tous les destinataires associés
            recRepo.deleteBySms_RefIn(refs);

            // Supprimer tous les SMS
            smsRepo.deleteAll(clientSms);
        }
    }
}
