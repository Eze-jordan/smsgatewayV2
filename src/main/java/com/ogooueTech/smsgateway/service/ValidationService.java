package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.model.Validation;
import com.ogooueTech.smsgateway.repository.ValidationRipository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MINUTES;

@Service
public class ValidationService {

    @Autowired
    private ValidationRipository validationRipository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Enregistre une nouvelle validation pour un Manager
     * - Génère un code aléatoire à 6 chiffres
     * - Définit la date de création et d’expiration (60 minutes)
     * - Sauvegarde la validation en base
     * - Envoie le code au Manager via NotificationService
     */
    public void enregister(Manager manager) {
        Validation validation = new Validation();
        validation.setManager(manager);

        Instant creation = Instant.now();
        validation.setCreation(creation);

        // Expiration : 60 minutes après la création
        Instant expiration = creation.plus(60, MINUTES);
        validation.setExpiration(expiration);

        // Génération d’un code aléatoire sur 6 chiffres
        Random random = new Random();
        int randomInteger = random.nextInt(999999);
        String code = String.format("%06d", randomInteger);

        validation.setCode(code);

        // Identifiant unique de la validation
        validation.setId("validation-" + UUID.randomUUID());

        // Sauvegarde et envoi
        this.validationRipository.save(validation);
        this.notificationService.envoyer(validation);
    }

    /**
     * Renvoyer un nouveau code UNIQUEMENT si l’ancien a expiré
     * - Si expiration atteinte : génère un nouveau code valide 10 minutes
     * - Sinon, lève une exception (car le code est encore valide)
     */
    public void renvoyerCode(Manager manager) {
        Validation validation = validationRipository.findByManager(manager)
                .orElseThrow(() -> new RuntimeException("Aucun code trouvé pour cet utilisateur"));

        Instant now = Instant.now();

        if (validation.getExpiration().isBefore(now)) {
            // Génère un nouveau code à 6 chiffres
            int randomInteger = new Random().nextInt(999999);
            String newCode = String.format("%06d", randomInteger);

            validation.setCode(newCode);
            validation.setCreation(now);
            validation.setExpiration(now.plus(10, MINUTES)); // code valide 10 minutes

            validationRipository.save(validation);
            notificationService.envoyer(validation);
        } else {
            throw new RuntimeException("Le code actuel est encore valide");
        }
    }

    /**
     * Renvoyer un nouveau code à un Manager
     * - Si aucune validation n’existe : en crée une
     * - Génère toujours un nouveau code valide 10 minutes
     */
    public void renvoyerNouveauCode(Manager manager) {
        Validation validation = validationRipository.findByManager(manager)
                .orElseGet(() -> {
                    Validation v = new Validation();
                    v.setManager(manager);
                    return v;
                });

        Instant now = Instant.now();

        // Nouveau code aléatoire
        int randomInteger = new Random().nextInt(999999);
        String code = String.format("%06d", randomInteger);

        validation.setCode(code);
        validation.setCreation(now);
        validation.setExpiration(now.plus(10, MINUTES));

        validationRipository.save(validation);
        notificationService.envoyer(validation);
    }

    /**
     * Récupère une validation à partir du code saisi
     * - Si le code est invalide ou absent, lève une exception
     */
    public Validation lireEnFonctionDuCode(String code) {
        return this.validationRipository.findByCode(code).orElseThrow(() ->
                new RuntimeException("votre code est invalide"));
    }

    /* ---------- Getters & Setters ---------- */
    public ValidationRipository getValidationRipository() {
        return validationRipository;
    }

    public void setValidationRipository(ValidationRipository validationRipository) {
        this.validationRipository = validationRipository;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
