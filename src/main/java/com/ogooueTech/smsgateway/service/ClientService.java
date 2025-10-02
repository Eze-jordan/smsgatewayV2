package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.ClientDTO;
import com.ogooueTech.smsgateway.dtos.SoldeNetResponse;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.mapper.ClientMapper;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.ogooueTech.smsgateway.service.NotificationService notificationService;
    private final com.ogooueTech.smsgateway.service.ClientsGroupsServiceImpl clientsGroupsServiceImpl; // AJOUT


    // Injection des dépendances via constructeur
    public ClientService(ClientRepository clientRepository, PasswordEncoder passwordEncoder, com.ogooueTech.smsgateway.service.NotificationService notificationService, com.ogooueTech.smsgateway.service.ClientsGroupsServiceImpl clientsGroupsServiceImpl) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.clientsGroupsServiceImpl = clientsGroupsServiceImpl;
    }

    // Compteur interne pour générer les ID clients (6 chiffres)
    // Initialisé à 700000 au démarrage
    private static final java.util.concurrent.atomic.AtomicLong LAST_CLIENT_ID =
            new java.util.concurrent.atomic.AtomicLong(700000);

    /**
     * Initialise le compteur LAST_CLIENT_ID avec le plus grand id existant en base.
     * Appelé automatiquement au démarrage grâce à @PostConstruct.
     */
    @jakarta.annotation.PostConstruct
    public void initClientLastId() {
        long max = 700000L;
        for (var c : clientRepository.findAll()) {
            try {
                if (c.getIdclients() != null && c.getIdclients().matches("\\d{6}")) {
                    long v = Long.parseLong(c.getIdclients());
                    if (v > max) max = v;
                }
            } catch (NumberFormatException ignored) {} // ignore les IDs non numériques
        }
        LAST_CLIENT_ID.set(max);
    }

    /**
     * Génère un nouvel ID client unique sur 6 chiffres
     */
    private String generateClientId() {
        String id;
        do {
            long next = LAST_CLIENT_ID.incrementAndGet(); // incrémente le compteur
            id = String.format("%06d", next); // format 6 chiffres avec zéros devant
        } while (clientRepository.existsById(id)); // vérifie l’unicité
        return id;
    }

    /* ---------- Génération clé API et mot de passe temporaire ---------- */

    /**
     * Génère une clé API unique de 48 caractères hexadécimaux (192 bits)
     */
    private String generateApiKey() {
        var sr = new java.security.SecureRandom();
        byte[] bytes = new byte[24]; // 192 bits = 24 octets
        String apiKey;
        do {
            sr.nextBytes(bytes);
            apiKey = java.util.HexFormat.of().formatHex(bytes);
        } while (clientRepository.existsByCleApi(apiKey)); // unicité en base
        return apiKey;
    }

    /**
     * Génère un mot de passe temporaire robuste
     * - contient au moins 1 majuscule, 1 minuscule, 1 chiffre et 1 spécial
     * - longueur totale 14 caractères
     */
    private String generateTempPassword() {
        String U = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // majuscules sans I et O
        String L = "abcdefghijkmnopqrstuvwxyz"; // minuscules sans l
        String D = "23456789"; // chiffres sans 0 et 1
        String S = "@#$%&*?!"; // caractères spéciaux
        java.security.SecureRandom r = new java.security.SecureRandom();

        // Ajoute 1 caractère obligatoire de chaque catégorie
        StringBuilder sb = new StringBuilder();
        sb.append(U.charAt(r.nextInt(U.length())));
        sb.append(L.charAt(r.nextInt(L.length())));
        sb.append(D.charAt(r.nextInt(D.length())));
        sb.append(S.charAt(r.nextInt(S.length())));

        // Complète avec des caractères aléatoires toutes catégories confondues
        String ALL = U + L + D + S;
        int targetLen = 14;
        while (sb.length() < targetLen) {
            sb.append(ALL.charAt(r.nextInt(ALL.length())));
        }

        // Mélange les caractères pour éviter un ordre prévisible
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }

    /**
     * Création d’un client par un Manager
     * - Vérifie l’unicité de l’email
     * - Génère un id client, une clé API et un mot de passe temporaire
     * - Sauvegarde le client
     * - Envoie les informations d’accès par email
     * @param dto données d’entrée
     * @return ClientDTO du client créé
     */
    public ClientDTO createByManager(ClientDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Payload manquant");
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new IllegalArgumentException("Email client obligatoire");
        if (clientRepository.existsByEmail(dto.getEmail()))
            throw new IllegalArgumentException("Un client avec cet email existe déjà");

        // Mapping DTO -> Entity (le DTO peut ne pas contenir de motDePasse)
        Client entity = ClientMapper.toEntity(dto);

        // Génère l’ID avant sauvegarde (champ NOT NULL en base)
        if (entity.getIdclients() == null || entity.getIdclients().isBlank()) {
            entity.setIdclients(generateClientId());
        }

        // Valeurs par défaut côté serveur
        if (entity.getRole() == null) entity.setRole(Role.CLIENT_USER);
        if (entity.getSoldeNet() == null) entity.setSoldeNet(0);
        if (entity.getStatutCompte() == null) entity.setStatutCompte(StatutCompte.ACTIF);

        // Génère la clé API
        String apiKey = generateApiKey();
        entity.setCleApi(apiKey);

        // Génère le mot de passe temporaire + hash
        String rawPassword = generateTempPassword();
        entity.setMotDePasse(passwordEncoder.encode(rawPassword));

        // Sauvegarde en base
        Client saved = clientRepository.save(entity);

        // AJOUT 👉 crée le groupe par défaut "Contacts" pour ce client
        clientsGroupsServiceImpl.createDefaultGroup(saved);

        // ensuite seulement on envoie l'email
        notificationService.envoyerAccesClient(saved, rawPassword);

        // Retourne le DTO
        return ClientMapper.toDto(saved);
    }

    /**
     * Récupère tous les clients en DTO
     */
    public List<ClientDTO> findAll() {
        return clientRepository.findAll().stream().map(ClientMapper::toDto).toList();
    }


    /**
     * Permet à un client de modifier son mot de passe
     * - Vérifie que le client existe
     * - Vérifie que l'ancien mot de passe est correct
     * - Encode et sauvegarde le nouveau mot de passe
     *
     * @param clientId identifiant du client
     * @param oldPassword ancien mot de passe (en clair)
     * @param newPassword nouveau mot de passe (en clair)
     */
    public void changePassword(String clientId, String oldPassword, String newPassword) {
        // Récupère le client
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));

        // Vérifie que l'ancien mot de passe correspond
        if (!passwordEncoder.matches(oldPassword, client.getMotDePasse())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }

        // Vérifie que le nouveau mot de passe n’est pas vide
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide");
        }

        // Encode et met à jour le nouveau mot de passe
        client.setMotDePasse(passwordEncoder.encode(newPassword));

        // Sauvegarde en base
        clientRepository.save(client);
    }


    /**
     * Récupère un client par son ID
     */
    public ClientDTO findById(String id) {
        Client e = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + id));
        return ClientMapper.toDto(e);
    }

    /**
     * Mise à jour partielle d’un client (hors champs contrôlés serveur)
     * - Vérifie unicité email
     * - Encode le mot de passe si fourni
     * - Ne permet pas de modifier cleApi, soldeNet, statutCompte
     */
    public ClientDTO patch(String id, ClientDTO dto) {
        Client e = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + id));

        // Vérifie unicité email si modifié
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(e.getEmail())
                && clientRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Un client avec cet email existe déjà");
        }

        // Met à jour uniquement les champs non nuls
        ClientMapper.updateEntityFromDto(dto, e);

        // Encode le mot de passe si fourni
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank()) {
            e.setMotDePasse(passwordEncoder.encode(dto.getMotDePasse()));
        }

        // Protège les champs côté serveur
        return ClientMapper.toDto(clientRepository.save(e));
    }

    /**
     * Supprime un client par son ID
     */
    public void delete(String id) {
        if (!clientRepository.existsById(id))
            throw new EntityNotFoundException("Client introuvable: " + id);
        clientRepository.deleteById(id);
    }

    /** Récupère la clé API via l'ID client. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public String getApiKeyByClientId(String clientId) {
        Client c = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));
        return c.getCleApi();
    }

    /** Récupère la clé API via l'email du client. */
    @Transactional(Transactional.TxType.SUPPORTS)
    public String getApiKeyByEmail(String email) {
        Client c = clientRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable avec l'email: " + email));
        return c.getCleApi();
    }
    @jakarta.transaction.Transactional(Transactional.TxType.SUPPORTS)
    public SoldeNetResponse getSoldeNetDtoByClientId(String clientId) {
        Client c = clientRepository.findById(clientId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Client introuvable: " + clientId));
        int v = c.getSoldeNet() == null ? 0 : c.getSoldeNet();
        return new com.ogooueTech.smsgateway.dtos.SoldeNetResponse(c.getIdclients(),
                c.getTypeCompte() != null ? c.getTypeCompte().name() : null, v);
    }

    /**
     * Suspend un client : il ne pourra plus rien faire (envoyer des SMS, etc.)
     * @param clientId identifiant du client à suspendre
     */
    public void suspendClient(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));

        if (client.getStatutCompte() == StatutCompte.SUSPENDU) {
            throw new IllegalArgumentException("Le client est déjà suspendu");
        }

        client.setStatutCompte(StatutCompte.SUSPENDU);
        clientRepository.save(client);

        notificationService.envoyerSuspensionClient(client);
    }
    /**
     * Réactive un client suspendu.
     * @param clientId identifiant du client
     */
    public void reactivateClient(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));

        if (client.getStatutCompte() == StatutCompte.ACTIF) {
            throw new IllegalArgumentException("Le client est déjà actif");
        }

        client.setStatutCompte(StatutCompte.ACTIF);
        clientRepository.save(client);

        notificationService.envoyerReactivationClient(client);
    }
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ClientDTO> searchByName(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return clientRepository.findByRaisonSocialeContainingIgnoreCase(name)
                .stream()
                .map(ClientMapper::toDto)
                .toList();
    }


    @Transactional(Transactional.TxType.SUPPORTS)
    public byte[] exportClientsToExcel() {
        List<Client> clients = clientRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Clients");

            // 🎨 Style d’entête
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Ligne d’entête
            Row header = sheet.createRow(0);
            String[] columns = {"ID Client", "Raison sociale", "Email", "Téléphone", "Ville", "Statut", "Type Compte", "Solde Net"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowIdx = 1;
            for (Client c : clients) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getIdclients());
                row.createCell(1).setCellValue(c.getRaisonSociale() != null ? c.getRaisonSociale() : "");
                row.createCell(2).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                row.createCell(3).setCellValue(c.getTelephone() != null ? c.getTelephone() : "");
                row.createCell(4).setCellValue(c.getVille() != null ? c.getVille() : "");
                row.createCell(5).setCellValue(c.getStatutCompte() != null ? c.getStatutCompte().name() : "");
                row.createCell(6).setCellValue(c.getTypeCompte() != null ? c.getTypeCompte().name() : "");
                row.createCell(7).setCellValue(c.getSoldeNet() != null ? c.getSoldeNet() : 0);
            }

            // Ajuste la largeur des colonnes
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Erreur export Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Archive un client : ses données restent en base mais il n’a plus accès au système
     */
    public void archiveClient(String clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable: " + clientId));

        if (client.getStatutCompte() == StatutCompte.ARCHIVE) {
            throw new IllegalArgumentException("Le client est déjà archivé");
        }

        client.setStatutCompte(StatutCompte.ARCHIVE);
        clientRepository.save(client);

    }
}
