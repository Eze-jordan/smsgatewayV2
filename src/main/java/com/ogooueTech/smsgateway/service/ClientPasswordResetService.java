package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.ForgotPasswordRequest;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.securite.JwtService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chargé de la réinitialisation du mot de passe d'un client.
 *
 * Le processus comprend deux étapes :
 * 1. génération et envoi d'un token temporaire ;
 * 2. validation du token et remplacement du mot de passe.
 */
@Service
@Transactional
public class ClientPasswordResetService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final int resetExpiryMinutes;

    /**
     * Injection des dépendances et de la durée de validité
     * des tokens de réinitialisation.
     *
     * La propriété peut être définie dans la configuration :
     *
     * app.security.reset-exp-minutes=30
     *
     * @param clientRepository repository des clients
     * @param passwordEncoder encodeur des mots de passe
     * @param notificationService service d'envoi des notifications
     * @param jwtService service de gestion des tokens JWT
     * @param resetExpiryMinutes durée de validité du token en minutes
     */
    public ClientPasswordResetService(
            ClientRepository clientRepository,
            PasswordEncoder passwordEncoder,
            NotificationService notificationService,
            JwtService jwtService,
            @Value("${app.security.reset-exp-minutes:30}")
            int resetExpiryMinutes
    ) {
        if (resetExpiryMinutes <= 0) {
            throw new IllegalArgumentException(
                    "La durée de validité du token doit être supérieure à zéro"
            );
        }

        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.resetExpiryMinutes = resetExpiryMinutes;
    }

    /**
     * Génère et envoie un token de réinitialisation lorsqu'un compte
     * correspond à l'adresse e-mail fournie.
     *
     * Aucun message d'erreur n'est généré lorsqu'aucun compte n'existe.
     * Cela empêche un attaquant de vérifier quelles adresses e-mail
     * sont inscrites dans l'application.
     *
     * Le contrôleur doit toujours retourner une réponse générique,
     * par exemple :
     *
     * "Si un compte correspond à cette adresse, un message a été envoyé."
     *
     * @param request demande contenant l'adresse e-mail
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null
                || request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "L'adresse e-mail est obligatoire"
            );
        }

        String email = request.getEmail().trim();

        clientRepository.findByEmail(email).ifPresent(client -> {
            String token = jwtService.generatePasswordResetToken(
                    client.getIdclients(),
                    resetExpiryMinutes
            );

            notificationService.envoyerResetClient(client, token);
        });
    }

    /**
     * Remplace le mot de passe du client associé au token.
     *
     * Le token doit :
     * - être valide ;
     * - ne pas être expiré ;
     * - avoir été généré pour une réinitialisation de mot de passe.
     *
     * @param token token de réinitialisation
     * @param newPassword nouveau mot de passe en clair
     */
    public void resetPassword(
            String token,
            String newPassword
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Le token de réinitialisation est obligatoire"
            );
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nouveau mot de passe est obligatoire"
            );
        }

        jwtService.assertValidPasswordResetToken(token.trim());

        String clientId = jwtService.extractSubjectFromPasswordResetToken(
                token.trim()
        );

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Lien de réinitialisation invalide ou expiré"
                ));

        client.setMotDePasse(passwordEncoder.encode(newPassword));

        clientRepository.save(client);
    }
}