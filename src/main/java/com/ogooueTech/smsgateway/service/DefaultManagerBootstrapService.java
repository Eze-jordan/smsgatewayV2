package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Service chargé de créer un manager par défaut
 * lorsque la table des managers est vide.
 *
 * Règles :
 * - si au moins un manager existe, aucune création n'est réalisée ;
 * - si aucun manager n'existe, le manager par défaut est créé ;
 * - le mot de passe est fourni par la configuration ;
 * - le mot de passe est encodé avec BCrypt ;
 * - aucun mot de passe n'est généré ;
 * - aucun e-mail n'est envoyé ;
 * - aucun doublon n'est créé après un redémarrage.
 */
@Service
public class DefaultManagerBootstrapService {

    private final ManagerRepository managerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final boolean enabled;
    private final String id;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String telephone;
    private final String password;
    private final String role;

    public DefaultManagerBootstrapService(
            ManagerRepository managerRepository,
            BCryptPasswordEncoder passwordEncoder,

            @Value("${app.bootstrap.default-manager.enabled:false}")
            boolean enabled,

            @Value("${app.bootstrap.default-manager.id:}")
            String id,

            @Value("${app.bootstrap.default-manager.nom:}")
            String nom,

            @Value("${app.bootstrap.default-manager.prenom:}")
            String prenom,

            @Value("${app.bootstrap.default-manager.email:}")
            String email,

            @Value("${app.bootstrap.default-manager.telephone:}")
            String telephone,

            @Value("${app.bootstrap.default-manager.password:}")
            String password,

            @Value("${app.bootstrap.default-manager.role:SUPER_ADMIN}")
            String role
    ) {
        this.managerRepository = managerRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.password = password;
        this.role = role;
    }

    /**
     * Crée le manager par défaut uniquement lorsque
     * aucun manager n'existe dans la base.
     */
    @Transactional
    public void ensureDefaultManagerExists() {
        /*
         * Permet de désactiver cette création automatique
         * depuis les variables d'environnement.
         */
        if (!enabled) {
            return;
        }

        /*
         * Si un manager quelconque existe déjà,
         * aucune création automatique n'est effectuée.
         */
        if (managerRepository.count() > 0) {
            return;
        }

        String normalizedId = requireValue(
                id,
                "L'identifiant du manager par défaut est obligatoire"
        );

        String normalizedNom = requireValue(
                nom,
                "Le nom du manager par défaut est obligatoire"
        );

        String normalizedPrenom = requireValue(
                prenom,
                "Le prénom du manager par défaut est obligatoire"
        );

        String normalizedEmail = requireValue(
                email,
                "L'adresse e-mail du manager par défaut est obligatoire"
        ).toLowerCase(Locale.ROOT);

        String normalizedTelephone = requireValue(
                telephone,
                "Le numéro de téléphone du manager par défaut est obligatoire"
        );

        String rawPassword = requireValue(
                password,
                "Le mot de passe du manager par défaut est obligatoire"
        );

        Role normalizedRole = parseRole(role);

        /*
         * Ton projet utilise actuellement des identifiants
         * manager composés de six chiffres.
         */
        if (!normalizedId.matches("\\d{6}")) {
            throw new IllegalStateException(
                    "L'identifiant du manager par défaut doit contenir exactement six chiffres"
            );
        }

        if (rawPassword.length() < 12) {
            throw new IllegalStateException(
                    "Le mot de passe du manager par défaut doit contenir au moins 12 caractères"
            );
        }

        /*
         * Protections supplémentaires contre les doublons.
         */
        if (managerRepository.existsById(normalizedId)) {
            return;
        }

        if (managerRepository.existsByEmail(normalizedEmail)) {
            return;
        }

        if (managerRepository.existsByNumeroTelephoneManager(
                normalizedTelephone
        )) {
            return;
        }

        Manager manager = new Manager();

        manager.setIdManager(normalizedId);
        manager.setNomManager(normalizedNom);
        manager.setPrenomManager(normalizedPrenom);
        manager.setEmail(normalizedEmail);
        manager.setNumeroTelephoneManager(normalizedTelephone);
        manager.setRole(normalizedRole);
        manager.setStatutCompte(StatutCompte.ACTIF);

        /*
         * Le mot de passe fourni dans la VM est directement
         * encodé avant son enregistrement en base.
         *
         * Aucun mot de passe n'est généré.
         */
        manager.setMotDePasseManager(
                passwordEncoder.encode(rawPassword)
        );

        /*
         * Enregistrement direct dans le repository.
         *
         * ManagerService.create() n'est volontairement pas appelé,
         * car il génère un nouveau mot de passe et envoie un e-mail.
         */
        managerRepository.saveAndFlush(manager);
    }

    /**
     * Convertit le rôle défini dans la configuration
     * vers l'énumération Role.
     */
    private Role parseRole(String value) {
        String normalizedRole = requireValue(
                value,
                "Le rôle du manager par défaut est obligatoire"
        ).toUpperCase(Locale.ROOT);

        try {
            return Role.valueOf(normalizedRole);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Rôle du manager par défaut invalide : "
                            + normalizedRole,
                    exception
            );
        }
    }

    /**
     * Vérifie qu'une valeur obligatoire est présente
     * puis supprime les espaces inutiles.
     */
    private String requireValue(
            String value,
            String errorMessage
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }

        return value.trim();
    }
}