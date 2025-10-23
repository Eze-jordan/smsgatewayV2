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

    // ========== SUPPRESSION ==========
    public void delete(String originalName) throws IOException {
        Optional<Document> docOpt = documentRepository.findByOriginalName(originalName);
        if (docOpt.isEmpty()) throw new IOException("Document introuvable.");

        Path filePath = fileStorageLocation.resolve(docOpt.get().getStoredName());
        Files.deleteIfExists(filePath);
        documentRepository.deleteByOriginalName(originalName);
    }
}
