package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.exception.BadRequestException;
import com.ogooueTech.smsgateway.exception.NotFoundException;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Emetteur;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.EmetteurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmetteurService {

    private final EmetteurRepository emetteurRepo;
    private final ClientRepository clientRepo;

    public EmetteurService(EmetteurRepository emetteurRepo, ClientRepository clientRepo) {
        this.emetteurRepo = emetteurRepo;
        this.clientRepo = clientRepo;
    }

    // -------------------------
    // 🟢 Création
    // -------------------------
    public Emetteur create(String clientId, String nom) {
        if (nom == null || nom.isBlank()) {
            throw new BadRequestException("Le nom de l’émetteur est obligatoire");
        }
        if (nom.length() > 11) {
            throw new BadRequestException("L’émetteur ne peut pas dépasser 11 caractères");
        }
        if (emetteurRepo.existsByClient_IdclientsAndNomIgnoreCase(clientId, nom)) {
            throw new BadRequestException("Cet émetteur existe déjà pour ce client");
        }

        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client introuvable: " + clientId));

        Emetteur e = new Emetteur();
        e.setNom(nom.trim());
        e.setClient(client);
        return emetteurRepo.save(e);
    }


    // -------------------------
    // 🟡 Modification
    // -------------------------
    public Emetteur update(String clientId, String emetteurId, String nouveauNom) {
        if (nouveauNom == null || nouveauNom.isBlank())
            throw new BadRequestException("Le nouveau nom est obligatoire");

        if (nouveauNom.length() > 11)
            throw new BadRequestException("Le nom ne peut pas dépasser 11 caractères");

        Emetteur e = emetteurRepo.findById(emetteurId)
                .orElseThrow(() -> new NotFoundException("Émetteur introuvable : " + emetteurId));

        // Vérifie que cet émetteur appartient bien au client connecté
        if (!e.getClient().getIdclients().equals(clientId)) {
            throw new BadRequestException("Cet émetteur n’appartient pas à ce client");
        }

        // Vérifie si un autre émetteur du même client porte déjà ce nom
        boolean exists = emetteurRepo.existsByClient_IdclientsAndNomIgnoreCase(clientId, nouveauNom);
        if (exists && !e.getNom().equalsIgnoreCase(nouveauNom)) {
            throw new BadRequestException("Un autre émetteur porte déjà ce nom pour ce client");
        }

        e.setNom(nouveauNom.trim());
        return emetteurRepo.save(e);
    }

    // -------------------------
    // 🔴 Suppression
    // -------------------------
    public void delete(String clientId, String emetteurId) {
        Emetteur e = emetteurRepo.findById(emetteurId)
                .orElseThrow(() -> new NotFoundException("Émetteur introuvable : " + emetteurId));

        if (!e.getClient().getIdclients().equals(clientId)) {
            throw new BadRequestException("Impossible de supprimer un émetteur appartenant à un autre client");
        }

        emetteurRepo.delete(e);
    }

    // -------------------------
    // 🔵 Liste
    // -------------------------
    public List<Emetteur> listByClient(String clientId) {
        return emetteurRepo.findByClient_Idclients(clientId);
    }
}
