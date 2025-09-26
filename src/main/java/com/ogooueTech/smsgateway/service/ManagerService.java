package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Manager;

import com.ogooueTech.smsgateway.repository.ManagerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private com.ogooueTech.smsgateway.service.NotificationService notificationService;



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
        dto.setStatutCompte(e.getStatutCompte());
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
        e.setStatutCompte(dto.getStatutCompte());
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
    }

    /* ---------- Métiers principaux ---------- */

    /**
     * Crée un nouveau Manager
     * - Vérifie unicité de l’email et du téléphone
     * - Attribue un rôle par défaut (ADMIN) si absent
     * - Génère un identifiant custom (6 chiffres)
     * - Enregistre le manager et envoie les identifiants après commit
     */
    public ManagerDTO create(ManagerDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Payload manquant");

        // Vérification unicité email
        if (dto.getEmail() != null && managerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un manager avec cet email existe déjà");
        }

        // Vérification unicité téléphone
        if (dto.getNumeroTelephoneManager() != null
                && managerRepository.existsByNumeroTelephoneManager(dto.getNumeroTelephoneManager())) {
            throw new IllegalArgumentException("Un manager avec ce numéro existe déjà");
        }

        // Rôle par défaut si non fourni
        if (dto.getRole() == null) {
            dto.setRole(Role.ADMIN);
        }

        Manager entity = toEntity(dto);

        // Statut par défaut = ACTIF
        if (entity.getStatutCompte() == null) {
            entity.setStatutCompte(StatutCompte.ACTIF);
        }

        // Génère un ID 6 chiffres si absent
        if (entity.getIdManager() == null || entity.getIdManager().isBlank()) {
            entity.setIdManager(generateCustomId());
        }

        // Génère et encode un mot de passe aléatoire
        String rawPassword = generatePassword(10);
        entity.setMotDePasseManager(passwordEncoder.encode(rawPassword));

        Manager saved = managerRepository.save(entity);

        // Envoi de l'email uniquement après que la transaction soit validée
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                notificationService.envoyerIdentifiantsManager(saved, rawPassword);
            }
        });

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

    /* ---------- Génération de mot de passe aléatoire ---------- */
    private String generatePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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



    /* ---------- Suppression ---------- */

    public void delete(String id) {
        if (!managerRepository.existsById(id)) {
            throw new EntityNotFoundException("Manager introuvable: " + id);
        }
        managerRepository.deleteById(id);
    }

    /**
     * Permet à un manager de modifier son mot de passe
     *
     * @param managerId identifiant du manager
     * @param oldPassword ancien mot de passe (en clair, fourni par l'utilisateur)
     * @param newPassword nouveau mot de passe (en clair, à encoder)
     */
    public void changePassword(String managerId, String oldPassword, String newPassword) {
        // Récupère le manager
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        // Vérifie l'ancien mot de passe
        if (!passwordEncoder.matches(oldPassword, manager.getMotDePasseManager())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }

        // Vérifie que le nouveau mot de passe n’est pas vide
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide");
        }

        // Encode et sauvegarde le nouveau mot de passe
        manager.setMotDePasseManager(passwordEncoder.encode(newPassword));
        managerRepository.save(manager);
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

    /**
     * Suspend un manager (il ne peut plus se connecter ni agir)
     */
    public Map<String, String> suspendManager(String managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        if (manager.getStatutCompte() == StatutCompte.SUSPENDU) {
            throw new IllegalArgumentException("Le manager est déjà suspendu");
        }

        manager.setStatutCompte(StatutCompte.SUSPENDU);
        managerRepository.save(manager);

        notificationService.envoyerSuspensionManager(manager);

        return Map.of(
                "status", "success",
                "message", "Le manager " + manager.getEmail() + " a été suspendu avec succès"
        );
    }

    /**
     * Réactive un manager suspendu
     */
    public Map<String, String> reactivateManager(String managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        if (manager.getStatutCompte() == StatutCompte.ACTIF) {
            throw new IllegalArgumentException("Le manager est déjà actif");
        }

        manager.setStatutCompte(StatutCompte.ACTIF);
        managerRepository.save(manager);

        notificationService.envoyerReactivationManager(manager);

        return Map.of(
                "status", "success",
                "message", "Le manager " + manager.getEmail() + " a été réactivé avec succès"
        );
    }

    /**
     * Archive un manager (soft delete)
     */
    public Map<String, String> archiveManager(String managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        if (manager.getStatutCompte() == StatutCompte.ARCHIVE) {
            throw new IllegalArgumentException("Le manager est déjà archivé");
        }

        manager.setStatutCompte(StatutCompte.ARCHIVE);
        managerRepository.save(manager);

        return Map.of(
                "status", "success",
                "message", "Le manager " + manager.getEmail() + " a été archivé avec succès"
        );
    }
    /**
     * Désarchive un manager (le remet en ACTIF)
     */
    public Map<String, String> unarchiveManager(String managerId) {
        Manager manager = managerRepository.findById(managerId)
                .orElseThrow(() -> new EntityNotFoundException("Manager introuvable: " + managerId));

        if (manager.getStatutCompte() != StatutCompte.ARCHIVE) {
            throw new IllegalArgumentException("Le manager n’est pas archivé");
        }

        manager.setStatutCompte(StatutCompte.ACTIF);
        managerRepository.save(manager);

        return Map.of(
                "status", "success",
                "message", "Le manager " + manager.getEmail() + " a été désarchivé et réactivé avec succès"
        );
    }


}
