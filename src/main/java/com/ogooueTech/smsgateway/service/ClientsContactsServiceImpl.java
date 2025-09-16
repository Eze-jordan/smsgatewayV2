package com.ogooueTech.smsgateway.service;

import org.apache.poi.ss.usermodel.*;
import com.ogooueTech.smsgateway.exception.BadRequestException;
import com.ogooueTech.smsgateway.exception.NotFoundException;
import com.ogooueTech.smsgateway.model.ClientsContacts;
import com.ogooueTech.smsgateway.model.ClientsGroups;
import com.ogooueTech.smsgateway.repository.ClientsContactsRepository;
import com.ogooueTech.smsgateway.repository.ClientsGroupsRepository;
import io.jsonwebtoken.io.IOException; // ⚠️ Import probablement inutile (collision avec java.io.IOException)
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

@Service // Indique que cette classe est un service Spring (logique métier)
@Transactional // Toutes les méthodes sont transactionnelles par défaut
public class ClientsContactsServiceImpl implements ClientsContactsService {

    // Dépendances injectées via le constructeur
    private final ClientsContactsRepository contactRepo;
    private final ClientsGroupsRepository groupRepo;

    // Injection par constructeur (meilleure pratique que @Autowired direct)
    public ClientsContactsServiceImpl(ClientsContactsRepository contactRepo,
                                      ClientsGroupsRepository groupRepo) {
        this.contactRepo = contactRepo;
        this.groupRepo = groupRepo;
    }

    /**
     * Ajouter un contact dans un groupe spécifique
     */
    @Override
    public ClientsContacts addToGroup(String groupId, String number, String name) {
        ClientsGroups group = findGroupOr404(groupId); // Vérifie si le groupe existe
        String normalized = normalizeNumber(number);   // Nettoyage du numéro
        validateNumber(normalized);                    // Vérification format du numéro

        String clientId = group.getClient().getIdclients();
        // Vérifie si ce numéro existe déjà pour ce client
        if (contactRepo.existsByClientIdAndContactNumberIgnoreCase(clientId, normalized)) {
            throw new BadRequestException("Ce numéro existe déjà pour ce client");
        }

        // Création du contact
        ClientsContacts c = new ClientsContacts();
        c.setClientsGroup(group);
        c.setClientId(clientId); // Id client explicite (sécurité et cohérence)
        c.setContactNumber(normalized);
        c.setContactName(normalizeName(name));
        return contactRepo.save(c); // Persiste en base
    }

    /**
     * Lister les contacts d’un groupe donné
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClientsContacts> listByGroup(String groupId) {
        assertGroupExists(groupId); // Vérifie que le groupe existe
        return contactRepo.findAllByClientsGroup_IdClientsGroupsOrderByCreatedAtDesc(groupId);
    }

    /**
     * Récupérer un contact par son ID
     */
    @Override
    @Transactional(readOnly = true)
    public ClientsContacts get(String contactId) {
        return contactRepo.findById(contactId)
                .orElseThrow(() -> new NotFoundException("Contact introuvable: " + contactId));
    }

    /**
     * Modifier partiellement un contact (patch)
     */
    @Override
    public ClientsContacts patch(String contactId, String number, String name) {
        ClientsContacts c = get(contactId); // Récupère le contact existant

        // Si le numéro est fourni, on le met à jour
        if (number != null) {
            String normalized = normalizeNumber(number);
            if (normalized.isBlank()) throw new BadRequestException("Le numéro ne peut pas être vide");
            validateNumber(normalized);

            // Vérifie si le nouveau numéro est différent de l’ancien
            if (!normalized.equalsIgnoreCase(c.getContactNumber())) {
                // Empêche les doublons pour le même client
                if (contactRepo.existsByClientIdAndContactNumberIgnoreCase(c.getClientId(), normalized)) {
                    throw new BadRequestException("Ce numéro existe déjà pour ce client");
                }
                c.setContactNumber(normalized);
            }
        }
        // Si le nom est fourni, on le met à jour
        if (name != null) c.setContactName(normalizeName(name));

        return contactRepo.save(c);
    }

    /**
     * Supprimer un contact par son ID
     */
    @Override
    public void delete(String contactId) {
        if (!contactRepo.existsById(contactId)) {
            throw new NotFoundException("Contact introuvable: " + contactId);
        }
        contactRepo.deleteById(contactId);
    }

    /**
     * Déplacer un contact d’un groupe vers un autre
     */
    @Override
    public ClientsContacts move(String contactId, String targetGroupId) {
        ClientsContacts c = get(contactId);
        ClientsGroups target = findGroupOr404(targetGroupId);

        String sourceClientId = c.getClientId();
        String targetClientId = target.getClient().getIdclients();
        // Vérifie que les deux groupes appartiennent au même client
        if (!sourceClientId.equals(targetClientId)) {
            throw new BadRequestException("Déplacement interdit : les groupes appartiennent à des clients différents");
        }

        // Déplacement logique : juste changer de groupe
        c.setClientsGroup(target);
        c.setClientId(targetClientId); // Cohérence clientId
        return contactRepo.save(c);
    }

    /**
     * Importer des contacts à partir d’un fichier CSV
     */
    @Override
    public ImportReportxls importCsvToGroup(String groupId, InputStream csvStream, Charset charset) {
        ClientsGroups group = findGroupOr404(groupId);
        String clientId = group.getClient().getIdclients();

        int total = 0, inserted = 0, duplicates = 0; // Statistiques
        List<String> duplicateNumbers = new java.util.ArrayList<>(); // Liste des doublons

        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(csvStream, charset))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                total++;
                // Ignore la première ligne si elle contient "number"
                if (first && line.toLowerCase().contains("number")) { first = false; continue; }
                first = false;

                // Découpe ligne CSV : numéro, nom
                String[] parts = line.split(",", -1);
                if (parts.length == 0) continue;
                String rawNumber = parts[0];
                String rawName = parts.length > 1 ? parts[1] : null;

                String normalized = normalizeNumber(rawNumber);
                if (normalized.isBlank()) continue; // ignore vide
                try {
                    validateNumber(normalized); // format
                } catch (BadRequestException e) {
                    duplicates++; duplicateNumbers.add(normalized + " (format invalide)");
                    continue;
                }

                // Vérifie doublons
                if (contactRepo.existsByClientIdAndContactNumberIgnoreCase(clientId, normalized)) {
                    duplicates++; duplicateNumbers.add(normalized);
                    continue;
                }

                // Crée et sauvegarde le contact
                ClientsContacts c = new ClientsContacts();
                c.setClientsGroup(group);
                c.setClientId(clientId);
                c.setContactNumber(normalized);
                c.setContactName(normalizeName(rawName));
                contactRepo.save(c);
                inserted++;
            }
        } catch (java.io.IOException io) {
            throw new BadRequestException("Impossible de lire le fichier CSV: " + io.getMessage());
        }

        // Retourne un rapport d’import
        return new ImportReportxls(groupId, total, inserted, duplicates, duplicateNumbers);
    }

    /**
     * Lister tous les contacts sans filtre
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClientsContacts> listAll() {
        return contactRepo.findAll();
    }

    /* ====================== Helpers ====================== */

    // Trouver un groupe ou lever NotFound
    private ClientsGroups findGroupOr404(String groupId) {
        return groupRepo.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Groupe introuvable: " + groupId));
    }

    // Vérifie si un groupe existe
    private void assertGroupExists(String groupId) {
        if (!groupRepo.existsById(groupId)) {
            throw new NotFoundException("Groupe introuvable: " + groupId);
        }
    }

    // Normalise un numéro (supprime espaces, tirets, parenthèses…)
    private String normalizeNumber(String number) {
        if (number == null) return "";
        return number.trim().replaceAll("[\\s\\-\\.()]", "");
    }

    // Vérifie la validité d’un numéro (8 à 15 chiffres, option +)
    private void validateNumber(String number) {
        if (!number.matches("^\\+?\\d{8,15}$")) {
            throw new BadRequestException("Format de numéro invalide (ex: +241066000000)");
        }
    }

    // Normalise un nom (supprime espaces, retourne null si vide)
    private String normalizeName(String name) {
        if (name == null) return null;
        String t = name.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Importer des contacts depuis un fichier Excel (.xlsx)
     */
    @Override
    public ImportReport importXlsxToGroup(String groupId, InputStream xlsxStream) throws IOException, java.io.IOException {
        ClientsGroups group = findGroupOr404(groupId);
        String clientId = group.getClient().getIdclients();

        int total = 0, inserted = 0, duplicates = 0;
        List<String> duplicateNumbers = new java.util.ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(xlsxStream)) {
            DataFormatter formatter = new DataFormatter(java.util.Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row == null) continue;

                // Ignore l’entête si première cellule contient "number"
                if (row.getRowNum() == 0) {
                    String h0 = cellToString(row, 0, formatter, evaluator).toLowerCase();
                    if (h0.contains("number") || h0.contains("numéro") || h0.contains("telephone")) {
                        continue;
                    }
                }

                total++;

                String rawNumber = cellToString(row, 0, formatter, evaluator);
                String rawName   = cellToString(row, 1, formatter, evaluator);

                String normalized = normalizeNumber(rawNumber);
                if (normalized.isBlank()) continue;

                try {
                    validateNumber(normalized);
                } catch (BadRequestException e) {
                    duplicates++; duplicateNumbers.add(normalized + " (format invalide)");
                    continue;
                }

                if (contactRepo.existsByClientIdAndContactNumberIgnoreCase(clientId, normalized)) {
                    duplicates++; duplicateNumbers.add(normalized);
                    continue;
                }

                // Création et sauvegarde du contact
                ClientsContacts c = new ClientsContacts();
                c.setClientsGroup(group);
                c.setClientId(clientId);
                c.setContactNumber(normalized);
                c.setContactName(normalizeName(rawName));
                contactRepo.save(c);
                inserted++;
            }
        }

        // Retour du rapport
        return new ImportReport(groupId, total, inserted, duplicates, duplicateNumbers);
    }

    /**
     * Convertit une cellule Excel en String (supporte numérique, string, formules…)
     * Utilise DataFormatter pour éviter les erreurs comme "Cannot get a STRING value from a NUMERIC cell"
     */
    private String cellToString(Row row, int cellIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        String v = formatter.formatCellValue(cell, evaluator);
        return v != null ? v.trim() : "";
    }

}
