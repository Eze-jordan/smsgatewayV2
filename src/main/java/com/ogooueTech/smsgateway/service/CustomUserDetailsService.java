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

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ManagerRepository managerRepository;
    private final ClientRepository clientRepository;

    public CustomUserDetailsService(ManagerRepository managerRepository, ClientRepository clientRepository) {
        this.managerRepository = managerRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 🔹 Recherche Manager
        Manager manager = managerRepository.findByEmail(email).orElse(null);
        if (manager != null) {
            if (manager.getStatutCompte() != StatutCompte.ACTIF) {
                throw new BadCredentialsException("⚠️ Compte manager " + manager.getStatutCompte().name().toLowerCase() + ". Connexion refusée.");
            }
            return new CustomUserDetails(
                    manager.getIdManager(),
                    manager.getEmail(),
                    manager.getMotDePasseManager(),
                    manager.getAuthorities(),
                    manager.getNomManager(),
                    manager.getRole().name(),
                    "MANAGER",
                    manager.getStatutCompte().name()
            );
        }

        // 🔹 Recherche Client
        Client client = clientRepository.findByEmail(email).orElse(null);
        if (client != null) {
            if (client.getStatutCompte() != StatutCompte.ACTIF) {
                throw new BadCredentialsException("⚠️ Compte client " + client.getStatutCompte().name().toLowerCase() + ". Connexion refusée.");
            }
            return new CustomUserDetails(
                    client.getIdclients(),
                    client.getEmail(),
                    client.getMotDePasse(),
                    client.getAuthorities(),
                    client.getRaisonSociale(),
                    client.getRole().name(),
                    client.getTypeCompte() != null ? client.getTypeCompte().name() : "INCONNU",
                    client.getStatutCompte().name()
            );
        }

        throw new UsernameNotFoundException("❌ Aucun compte trouvé pour : " + email);
    }
}
