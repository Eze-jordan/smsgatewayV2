package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.model.Validation;
import com.ogooueTech.smsgateway.repository.ManagerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ManagerService implements UserDetailsService {

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ValidationService validationService;

    /* ---------- Méthodes utilitaires de mapping ---------- */

    // Conversion Entité -> DTO
    private ManagerDTO toDto(Manager e) {
        if (e == null) return null;
        ManagerDTO dto = new ManagerDTO();
        dto.setIdManager(e.getIdManager());
        dto.setNomManager(e.getNomManager());
        dto.setPrenomManager(e.getPrenomManager());
        dto.setEmail(e.getEmail());
        dto.setNumeroTelephoneManager(e.getNumeroTelephoneManager());
        dto.setRole(e.getRole());
        dto.setActif(e.isActif());
        // ⚠️ motDePasseManager est WRITE_ONLY => jamais renvoyé côté API
        return dto;
    }

    // Conversion DTO -> Entité
    private Manager toEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager e = new Manager();
        e.setNomManager(dto.getNomManager());
        e.setPrenomManager(dto.getPrenomManager());
        e.setEmail(dto.getEmail());
        e.setNumeroTelephoneManager(dto.getNumeroTelephoneManager());
        e.setRole(dto.getRole());
        // Mot de passe encodé si fourni
        if (dto.getMotDePasseManager() != null) {
            e.setMotDePasseManager(passwordEncoder.encode(dto.getMotDePasseManager()));
        }
        // actif: par défaut false dans l'entité
        return e;
    }

    // Mise à jour partielle d’une entité Manager depuis un DTO
    private void updateEntityFromDto(ManagerDTO dto, Manager e) {
        if (dto.getNomManager() != null) e.setNomManager(dto.getNomManager());
        if (dto.getPrenomManager() != null) e.setPrenomManager(dto.getPrenomManager());
        if (dto.getEmail() != null) e.setEmail(dto.getEmail());
        if (dto.getNumeroTelephoneManager() != null) e.setNumeroTelephoneManager(dto.getNumeroTelephoneManager());
        if (dto.getRole() != null) e.setRole(dto.getRole());
        if (dto.getMotDePasseManager() != null) {
            e.setMotDePasseManager(passwordEncoder.encode(dto.getMotDePasseManager()));
        }
        if (dto.getActif() != null) e.setActif(dto.getActif());
    }

    /* ---------- Métiers principaux ---------- */

    /**
     * Crée un nouveau Manager
     * - Vérifie unicité de l’email
     * - Attribue un rôle par défaut (ADMIN) si absent
     * - Génère un identifiant custom (6 chiffres)
     * - Enregistre le manager et déclenche un code d’activation
     */
    public ManagerDTO create(ManagerDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Payload manquant");
        if (dto.getEmail() != null && managerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un manager avec cet email existe déjà");
        }

        // Rôle par défaut si non fourni
        if (dto.getRole() == null) {
            dto.setRole(Role.ADMIN);
        }

        Manager entity = toEntity(dto);

        // Génère un ID 6 chiffres si absent
        if (entity.getIdManager() == null || entity.getIdManager().isBlank()) {
            entity.setIdManager(generateCustomId());
        }

        Manager saved = managerRepository.save(entity);

        // Déclenchement d’un code d’activation
        validationService.enregister(saved);

        return toDto(saved);
    }

    /* ---------- Génération d’ID custom pour Manager ---------- */

    // Compteur thread-safe ; valeur initiale par défaut = 500000
    private static final java.util.concurrent.atomic.AtomicLong LAST_ID = new java.util.concurrent.atomic.AtomicLong(500000);

    @jakarta.annotation.PostConstruct
    public void initLastId() {
        // Calage du compteur sur le max existant en base
        var all = managerRepository.findAll();
        long max = 500000L;
        for (Manager m : all) {
            try {
                if (m.getIdManager() != null && m.getIdManager().matches("\\d{6}")) {
                    long val = Long.parseLong(m.getIdManager());
                    if (val > max) max = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        LAST_ID.set(max);
    }

    // Génère un nouvel identifiant Manager unique (6 chiffres)
    private String generateCustomId() {
        String id;
        do {
            long next = LAST_ID.incrementAndGet(); // thread-safe
            id = String.format("%06d", next);
        } while (managerRepository.existsById(id)); // boucle si collision improbable
        return id;
    }

    /* ---------- Lecture des managers ---------- */

    @Transactional(readOnly = true)
    public List<ManagerDTO> findAll() {
        return managerRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ManagerDTO findById(String id) {
        Manager e = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + id));
        return toDto(e);
    }

    /* ---------- Mise à jour ---------- */

    /**
     * Mise à jour partielle d’un Manager
     * - Vérifie unicité de l’email si modifié
     * - Met à jour uniquement les champs fournis
     */
    public ManagerDTO patch(String id, ManagerDTO dto) {
        Manager e = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + id));

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(e.getEmail())
                && managerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un manager avec cet email existe déjà");
        }

        updateEntityFromDto(dto, e);
        return toDto(managerRepository.save(e));
    }

    /* ---------- Activation / Désactivation ---------- */

    // Active un Manager
    public ManagerDTO activate(String id) {
        Manager e = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + id));
        e.setActif(true);
        return toDto(managerRepository.save(e));
    }

    // Désactive un Manager
    public ManagerDTO deactivate(String id) {
        Manager e = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + id));
        e.setActif(false);
        return toDto(managerRepository.save(e));
    }

    /* ---------- Suppression ---------- */

    public void delete(String id) {
        if (!managerRepository.existsById(id)) {
            throw new EntityNotFoundException("Manager introuvable: " + id);
        }
        managerRepository.deleteById(id);
    }

    /* ---------- Validation par code ---------- */

    /**
     * Active un Manager via un code de validation
     * - Vérifie la validité et l’expiration du code
     * - Active le compte si valide
     */
    public void activation(Map<String, String> activation) {
        Validation validation = validationService.lireEnFonctionDuCode(activation.get("code"));
        if (Instant.now().isAfter(validation.getExpiration())) {
            throw new RuntimeException("Votre code a expiré");
        }

        Manager ManagerActiver = managerRepository.findById(validation.getManager().getIdManager())
                .orElseThrow(() -> new RuntimeException("Utilisateur inconnu"));

        ManagerActiver.setActif(true);
        managerRepository.save(ManagerActiver);
    }

    /* ---------- Spring Security (authentification) ---------- */

    /**
     * Chargement d’un Manager par son email pour l’authentification
     */
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.managerRepository.findByEmail(username).orElseThrow(()
                -> new UsernameNotFoundException(
                "Aucun utilisateur ne correspond à cet identifiant"
        ));
    }

}
