package com.ogooueTech.smsgateway.service;


import com.ogooueTech.smsgateway.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SmsConsumptionService {

    private final ClientRepository clientRepository;

    public SmsConsumptionService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public void incrementSoldeNet(UUID clientId) {
        var c = clientRepository.findById(String.valueOf(clientId))
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable: " + clientId));
        // (Option: vérifier type POSTPAYE + compte ACTIF)
        c.setSoldeNet(c.getSoldeNet() + 1);
        clientRepository.save(c);
    }
}
