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
@Service
@Transactional
public class ClientPasswordResetService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    @Value("${app.security.reset-exp-minutes:30}")
    private int resetExpiryMinutes;

    public ClientPasswordResetService(ClientRepository clientRepository,
                                      PasswordEncoder passwordEncoder,
                                      NotificationService notificationService,
                                      JwtService jwtService) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    /** Étape 1 : génère et envoie un token de réinit */
    public void forgotPassword(ForgotPasswordRequest req) {
        Client client = clientRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Aucun compte associé à cet email"));
        String token = jwtService.generatePasswordResetToken(client.getIdclients(), resetExpiryMinutes);
        notificationService.envoyerResetClient(client, token);
    }

    public void resetPassword(String token, String newPassword) {
        jwtService.assertValidPasswordResetToken(token);
        String clientId = jwtService.extractSubjectFromPasswordResetToken(token);

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        client.setMotDePasse(passwordEncoder.encode(newPassword));
        clientRepository.save(client);
    }

}
