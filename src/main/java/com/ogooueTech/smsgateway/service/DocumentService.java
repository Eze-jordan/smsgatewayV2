package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.Document;
import com.ogooueTech.smsgateway.repository.DocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class DocumentService {

    private final Path fileStorageLocation = Paths.get("src/main/resources/static/documents")
            .toAbsolutePath().normalize();

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) throws IOException {
        this.documentRepository = documentRepository;
        Files.createDirectories(fileStorageLocation);
    }

    // ========== UPLOAD ==========
    public Document upload(MultipartFile file) throws IOException {
        String originalName = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");

        if (documentRepository.findByOriginalName(originalName).isPresent()) {
            throw new IOException("Un document avec ce nom existe déjà : " + originalName);
        }

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path target = fileStorageLocation.resolve(storedName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        Document document = new Document(storedName, originalName, file.getSize(), contentType);
        return documentRepository.save(document);
    }


    // ========== LISTE ==========
    public List<Document> listAll() {
        return documentRepository.findAll();
    }

    // ========== TÉLÉCHARGER / VISUALISER ==========
    public Resource load(String originalName) throws IOException {
        Optional<Document> docOpt = documentRepository.findByOriginalName(originalName);
        if (docOpt.isEmpty()) throw new IOException("Document introuvable en base.");

        String storedName = docOpt.get().getStoredName();
        Path path = fileStorageLocation.resolve(storedName).normalize();

        if (!Files.exists(path)) throw new IOException("Fichier introuvable sur le disque.");

        return new UrlResource(path.toUri());
    }
    // ========== MODIFIER / REMPLACER UN DOCUMENT ==========
    public Document updateDocument(String originalName, MultipartFile newFile) throws IOException {
        Optional<Document> docOpt = documentRepository.findByOriginalName(originalName);
        if (docOpt.isEmpty()) {
            throw new IOException("Document introuvable pour modification : " + originalName);
        }

        Document existingDoc = docOpt.get();

        // Supprimer l'ancien fichier sur le disque
        Path oldFilePath = fileStorageLocation.resolve(existingDoc.getStoredName()).normalize();
        Files.deleteIfExists(oldFilePath);

        // Enregistrer le nouveau fichier
        String newOriginalName = Objects.requireNonNull(newFile.getOriginalFilename()).replace(" ", "_");
        String newStoredName = UUID.randomUUID() + "_" + newOriginalName;
        Path target = fileStorageLocation.resolve(newStoredName);

        Files.copy(newFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Mettre à jour les informations du document
        existingDoc.setOriginalName(newOriginalName);
        existingDoc.setStoredName(newStoredName);
        existingDoc.setSize(newFile.getSize());
        existingDoc.setContentType(
                newFile.getContentType() != null ? newFile.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE
        );

        // Sauvegarder les modifications
        return documentRepository.save(existingDoc);
    }

    // ========== SUPPRESSION ROBUSTE ==========
    public Map<String, Object> delete(String originalName) throws IOException {
        List<Document> docs = documentRepository.findAll()
                .stream()
                .filter(d -> d.getOriginalName().equals(originalName))
                .toList();

        if (docs.isEmpty()) {
            throw new IOException("Aucun document trouvé avec le nom : " + originalName);
        }

        int deletedFiles = 0;

        for (Document doc : docs) {
            Path filePath = fileStorageLocation.resolve(doc.getStoredName());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                deletedFiles++;
            }
            documentRepository.delete(doc);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("deletedDocuments", docs.size());
        response.put("deletedFiles", deletedFiles);
        response.put("message", "Suppression effectuée avec succès.");
        return response;
    }

}
