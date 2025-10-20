package com.ogooueTech.smsgateway.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
@RestController
@RequestMapping("/api/V1")
@Tag(name = "Documentation SMS Gateway", description = "Télécharger la documentation api ")
public class DownloadController {

    private final Path fileStorageLocation = Paths.get("src/main/resources/static/documents")
            .toAbsolutePath().normalize();

    // Pour visualiser dans le navigateur
    @GetMapping("/view/{fileName:.+}")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName) {
        return handleFile(fileName, false);
    }

    // Pour télécharger le fichier
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        return handleFile(fileName, true);
    }

    private ResponseEntity<Resource> handleFile(String fileName, boolean forceDownload) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            String contentType = determineContentType(fileName);

            if (forceDownload) {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", fileName);
            } else {
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentDispositionFormData("inline", fileName);
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String fileName) {
        // Même méthode que précédemment
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "txt": return "text/plain";
            case "html": return "text/html";
            default: return "application/octet-stream";
        }
    }
}