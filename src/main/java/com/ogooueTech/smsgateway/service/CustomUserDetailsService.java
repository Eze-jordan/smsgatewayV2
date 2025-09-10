package com.ogooueTech.smsgateway.service;


import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import com.ogooueTech.smsgateway.securite.CustomUserDetails;
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
        Manager manager = managerRepository.findByEmail(email).orElse(null);
        if (manager != null) {
            return new CustomUserDetails(
                    manager.getEmail(),
                    manager.getMotDePasseManager(),
                    manager.getAuthorities(),
                    manager.getNomManager(),
                    manager.getRole().name(),
                    false // ✅ pas d’abonnement pour Utilisateur
            );
        }

        Client client = clientRepository.findByEmail(email).orElse(null);
        if (client != null) {
            return new CustomUserDetails(
                    client.getEmail(),
                    client.getMotDePasse(),
                    client.getAuthorities(),
                    client.getRaisonSociale(),
                    client.getRole().name(),
                    false // ✅ pas d’abonnement pour Utilisateur
            );
        }


        System.out.println("Connexion en tant que structure sanitaire : " + manager.getEmail());
        throw new UsernameNotFoundException("Aucun compte trouvé pour : " + email);
    }

}

