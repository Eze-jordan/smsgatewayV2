package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.exception.BadRequestException;
import com.ogooueTech.smsgateway.exception.NotFoundException;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.ClientsGroups;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.ClientsGroupsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientsGroupsServiceImpl implements ClientsGroupsService {

    private final ClientsGroupsRepository groupsRepo;
    private final ClientRepository clientRepo;

    public ClientsGroupsServiceImpl(ClientsGroupsRepository groupsRepo, ClientRepository clientRepo) {
        this.groupsRepo = groupsRepo;
        this.clientRepo = clientRepo;
    }

    /**
     * Création d’un groupe personnalisé pour un client
     * @param clientId id du client
     * @param nom nom du groupe
     * @param description description facultative
     * @return le groupe créé
     */
    @Override
    @Transactional
    public ClientsGroups create(String clientId, String nom, String description) {
        if (nom == null || nom.isBlank()) {
            throw new BadRequestException("Le nom du groupe est obligatoire");
        }

        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client introuvable: " + clientId));

        // 🔒 Vérifie unicité du nom pour ce client
        if (groupsRepo.existsByClient_IdclientsAndNomGroupeIgnoreCase(clientId, nom.trim())) {
            throw new BadRequestException("Un groupe portant ce nom existe déjà pour ce client");
        }

        ClientsGroups entity = new ClientsGroups();
        entity.setClient(client);
        entity.setNomGroupe(nom.trim());
        entity.setDescriptionGroupe(description);

        return groupsRepo.save(entity);


    }

    /**
     * Liste de tous les groupes existants (tous clients confondus)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClientsGroups> listAll() {
        return groupsRepo.findAll();
    }

    /**
     * Liste les groupes d’un client donné
     */
    @Override
    @Transactional(readOnly = true)
    public List<ClientsGroups> listByClient(String clientId) {
        return groupsRepo.findAllByClient_Idclients(clientId);
    }

    /**
     * Récupère un groupe par son id
     */
    @Override
    @Transactional(readOnly = true)
    public ClientsGroups get(String id) {
        return groupsRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Groupe introuvable: " + id));
    }

    /**
     * Mise à jour (partielle) d’un groupe
     */
    @Override
    @Transactional
    public ClientsGroups patch(String id, String nom, String description) {
        ClientsGroups entity = get(id); // récupère aussi clientId via entity.getClient().getIdclients()

        if (nom != null) {
            String trimmed = nom.trim();
            if (trimmed.isBlank()) {
                throw new BadRequestException("Le nom ne peut pas être vide");
            }

            String clientId = entity.getClient().getIdclients();

            // 🔒 Vérifie unicité du nom (hors groupe actuel)
            boolean nameTaken = groupsRepo
                    .existsByClient_IdclientsAndNomGroupeIgnoreCaseAndIdClientsGroupsNot(
                            clientId, trimmed, entity.getIdClientsGroups()
                    );
            if (nameTaken) {
                throw new BadRequestException("Un groupe portant ce nom existe déjà pour ce client");
            }
            entity.setNomGroupe(trimmed);
        }

        if (description != null) {
            entity.setDescriptionGroupe(description);
        }

        return groupsRepo.save(entity);
    }

    /**
     * Supprime un groupe
     */
    @Override
    @Transactional
    public void delete(String id) {
        ClientsGroups entity = get(id);
        groupsRepo.delete(entity);
    }

    /**
     * 🚀 Création automatique d’un groupe par défaut
     *
     * Cette méthode est pensée pour être appelée lors de la création d’un client.
     * Elle garantit que chaque nouveau client dispose d’un groupe initial.
     *
     * @param client l’entité Client fraichement créée
     * @return le groupe créé ou déjà existant
     */
    @Transactional
    public ClientsGroups createDefaultGroup(Client client) {
        // Nom du groupe par défaut
        String defaultName = "Default_group";

        // Vérifie si un groupe par défaut existe déjà pour ce client
        if (groupsRepo.existsByClient_IdclientsAndNomGroupeIgnoreCase(client.getIdclients(), defaultName)) {
            return groupsRepo.findAllByClient_Idclients(client.getIdclients())
                    .stream()
                    .filter(g -> g.getNomGroupe().equalsIgnoreCase(defaultName))
                    .findFirst()
                    .orElseThrow(); // Sécurité (improbable)
        }

        // Création du groupe par défaut
        ClientsGroups entity = new ClientsGroups();
        entity.setClient(client);
        entity.setNomGroupe(defaultName);
        entity.setDescriptionGroupe("Groupe des contacts");

        return groupsRepo.save(entity);
    }
}
