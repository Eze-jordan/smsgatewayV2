package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.securite.CustomUserDetails;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service utilisé par Spring Security pour charger un utilisateur
 * à partir de son adresse e-mail.
 *
 * Deux catégories de comptes sont prises en charge :
 * - les managers ;
 * - les clients.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    /**
     * Message volontairement générique afin de ne pas révéler :
     * - si un compte existe ;
     * - le type du compte ;
     * - son statut interne.
     */
    private static final String AUTHENTICATION_FAILURE_MESSAGE =
            "Identifiants invalides ou compte indisponible";

    private final ManagerRepository managerRepository;
    private final ClientRepository clientRepository;

    /**
     * Injection des repositories.
     *
     * @param managerRepository repository des managers
     * @param clientRepository repository des clients
     */
    public CustomUserDetailsService(
            ManagerRepository managerRepository,
            ClientRepository clientRepository
    ) {
        this.managerRepository = managerRepository;
        this.clientRepository = clientRepository;
    }

    /**
     * Recherche d'abord un manager, puis un client, à partir
     * de l'adresse e-mail fournie par Spring Security.
     *
     * @param email adresse e-mail utilisée comme identifiant
     * @return informations de sécurité de l'utilisateur
     * @throws UsernameNotFoundException lorsqu'aucun compte ne correspond
     */
    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                    AUTHENTICATION_FAILURE_MESSAGE
            );
        }

        String normalizedEmail = email.trim();

        Manager manager = managerRepository
                .findByEmail(normalizedEmail)
                .orElse(null);

        if (manager != null) {
            assertActive(manager.getStatutCompte());

            String managerRole = manager.getRole() == null
                    ? "INCONNU"
                    : manager.getRole().name();

            return new CustomUserDetails(
                    manager.getIdManager(),
                    manager.getEmail(),
                    manager.getMotDePasseManager(),
                    manager.getAuthorities(),
                    manager.getNomManager(),
                    managerRole,
                    "MANAGER",
                    manager.getStatutCompte().name()
            );
        }

        Client client = clientRepository
                .findByEmail(normalizedEmail)
                .orElse(null);

        if (client != null) {
            assertActive(client.getStatutCompte());

            String clientRole = client.getRole() == null
                    ? "INCONNU"
                    : client.getRole().name();

            String accountType = client.getTypeCompte() == null
                    ? "INCONNU"
                    : client.getTypeCompte().name();

            return new CustomUserDetails(
                    client.getIdclients(),
                    client.getEmail(),
                    client.getMotDePasse(),
                    client.getAuthorities(),
                    client.getRaisonSociale(),
                    clientRole,
                    accountType,
                    client.getStatutCompte().name()
            );
        }

        throw new UsernameNotFoundException(
                AUTHENTICATION_FAILURE_MESSAGE
        );
    }

    /**
     * Vérifie que le compte est actif.
     *
     * Le message retourné reste volontairement générique pour éviter
     * qu'un attaquant découvre le statut du compte.
     *
     * @param accountStatus statut du compte
     */
    private void assertActive(StatutCompte accountStatus) {
        if (accountStatus != StatutCompte.ACTIF) {
            throw new BadCredentialsException(
                    AUTHENTICATION_FAILURE_MESSAGE
            );
        }
    }
}