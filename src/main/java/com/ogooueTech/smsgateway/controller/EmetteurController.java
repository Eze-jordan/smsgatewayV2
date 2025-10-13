package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.CreateEmetteurRequest;
import com.ogooueTech.smsgateway.dtos.EmetteurResponse;
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
    public ResponseEntity<EmetteurResponse> create(@RequestBody CreateEmetteurRequest req) {
        Emetteur emetteur = service.create(req.clientId(), req.nom());
        // Transformer en DTO
        EmetteurResponse response = new EmetteurResponse(
                emetteur.getId(),
                emetteur.getNom(),
                emetteur.getCreatedAt()
        );
        return ResponseEntity.ok(response);
    }
    @PutMapping("/{clientId}/{id}")
    @Operation(summary = "Modifier un émetteur d’un client spécifique")
    public ResponseEntity<EmetteurResponse> update(
            @PathVariable("clientId") String clientId,
            @PathVariable("id") String emetteurId,
            @RequestParam String nouveauNom
    ) {
        Emetteur updated = service.update(clientId, emetteurId, nouveauNom);
        EmetteurResponse response = new EmetteurResponse(updated.getId(), updated.getNom(), updated.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{clientId}/{id}")
    @Operation(summary = "Supprimer un émetteur d’un client spécifique")
    public ResponseEntity<Void> delete(
            @PathVariable("clientId") String clientId,
            @PathVariable("id") String emetteurId
    ) {
        service.delete(clientId, emetteurId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Lister les émetteurs d’un client")
    public ResponseEntity<List<EmetteurResponse>> listByClient(@PathVariable String clientId) {
        List<Emetteur> emetteurs = service.listByClient(clientId);
        List<EmetteurResponse> response = emetteurs.stream()
                .map(e -> new EmetteurResponse(e.getId(), e.getNom(), e.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(response);
    }

}
