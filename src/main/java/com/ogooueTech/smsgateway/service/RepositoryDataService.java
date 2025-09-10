package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.RepositoryData;
import com.ogooueTech.smsgateway.repository.RepositoryDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RepositoryDataService {

    private final RepositoryDataRepository repository;

    // Injection du repository via constructeur
    public RepositoryDataService(RepositoryDataRepository repository) {
        this.repository = repository;
    }

    // 🔍 Récupérer toutes les données appartenant à une catégorie donnée
    public ResponseEntity<List<RepositoryData>> getAllByCategory(String refCategory) {
        return ResponseEntity.ok(repository.findByRefCategory(refCategory));
    }

    // 🔍 Récupérer une donnée par son ID
    public ResponseEntity<?> getById(Long refID) {
        Optional<RepositoryData> data = repository.findById(refID);
        if (data.isPresent()) {
            return ResponseEntity.ok(data.get());
        } else {
            return ResponseEntity.status(404).body("Donnée non trouvée pour l'ID " + refID);
        }
    }

    // 🔍 Recherche par mot-clé dans toutes les colonnes textuelles (keyValue, value1...value4)
    public ResponseEntity<List<RepositoryData>> search(String keyword) {
        List<RepositoryData> results = repository
                .findByKeyValueContainingIgnoreCaseOrValue1ContainingIgnoreCaseOrValue2ContainingIgnoreCaseOrValue3ContainingIgnoreCaseOrValue4ContainingIgnoreCase(
                        keyword, keyword, keyword, keyword, keyword);
        return ResponseEntity.ok(results);
    }

    // ✅ Créer une nouvelle entrée dans la base
    public ResponseEntity<?> create(RepositoryData repositoryData) {
        try {
            RepositoryData saved = repository.save(repositoryData);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création : " + e.getMessage());
        }
    }

    // ✅ Créer plusieurs entrées en une seule opération (insertion en lot)
    public ResponseEntity<?> createBatch(List<RepositoryData> dataList) {
        try {
            List<RepositoryData> saved = repository.saveAll(dataList);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création en lot : " + e.getMessage());
        }
    }

    // ♻️ Mettre à jour une entrée existante identifiée par son ID
    public ResponseEntity<?> update(Long refID, RepositoryData updatedData) {
        Optional<RepositoryData> existing = repository.findById(refID);
        if (existing.isPresent()) {
            RepositoryData data = existing.get();
            // Mise à jour des champs
            data.setKeyValue(updatedData.getKeyValue());
            data.setValue1(updatedData.getValue1());
            data.setValue2(updatedData.getValue2());
            data.setValue3(updatedData.getValue3());
            data.setValue4(updatedData.getValue4());
            data.setRefCategory(updatedData.getRefCategory());
            return ResponseEntity.ok(repository.save(data));
        } else {
            return ResponseEntity.status(404).body("Aucune donnée trouvée avec l'ID : " + refID);
        }
    }

    // ❌ Supprimer une donnée par son ID
    public ResponseEntity<?> delete(Long refID) {
        if (repository.existsById(refID)) {
            repository.deleteById(refID);
            return ResponseEntity.ok("Suppression réussie");
        } else {
            return ResponseEntity.status(404).body("Donnée introuvable pour suppression");
        }
    }

    // 🔹 Récupérer toutes les données disponibles
    public ResponseEntity<List<RepositoryData>> getAll() {
        List<RepositoryData> list = repository.findAll();
        return ResponseEntity.ok(list);
    }

    // 🔹 Récupérer la liste des catégories distinctes (évite doublons)
    public ResponseEntity<List<String>> getAllDistinctRefCategories() {
        List<String> categories = repository.findDistinctRefCategory();
        return ResponseEntity.ok(categories);
    }

}
