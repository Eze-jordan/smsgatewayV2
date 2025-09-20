package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.CreateEmetteurRequest;
import com.ogooueTech.smsgateway.model.Emetteur;
import com.ogooueTech.smsgateway.service.EmetteurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/V1/emetteurs")
@Tag(name = "Émetteurs", description = "Gestion des alias expéditeur SMS")
public class EmetteurController {

    private final EmetteurService service;

    public EmetteurController(EmetteurService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Créer un nouvel émetteur pour un client")
    public ResponseEntity<Emetteur> create(@RequestBody CreateEmetteurRequest req) {
        Emetteur emetteur = service.create(req.clientId(), req.nom());
        return ResponseEntity.ok(emetteur);
    }


    @GetMapping("/client/{clientId}")
    @Operation(summary = "Lister les émetteurs d’un client")
    public ResponseEntity<List<Emetteur>> listByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(service.listByClient(clientId));
    }
}
