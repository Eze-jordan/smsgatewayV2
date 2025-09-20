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

    public List<Emetteur> listByClient(String clientId) {
        return emetteurRepo.findByClient_Idclients(clientId);
    }
}
