package com.ogooueTech.smsgateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/V1/documents")
@Tag(name = "Gestion des Documents", description = "API pour uploader et télécharger des documents")
public class DocumentController {

    private final Path fileStorageLocation = Paths.get("src/main/resources/static/documents")
            .toAbsolutePath().normalize();

    public DocumentController() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    // Upload un document
    @PostMapping("/upload")
    @Operation(summary = "Uploader un document", description = "Téléverse un fichier vers le serveur")
    public ResponseEntity<UploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // Valider le fichier
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new UploadResponse("Le fichier est vide", null));
            }

            // Nettoyer le nom du fichier
            String originalFileName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName.replace(" ", "_");

            // Copier le fichier vers le dossier de destination
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            UploadResponse response = new UploadResponse(
                    "Fichier uploadé avec succès",
                    new FileInfo(fileName, originalFileName, file.getSize())
            );

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse("Erreur lors de l'upload: " + ex.getMessage(), null));
        }
    }

    // Upload multiple de documents
    @PostMapping("/upload-multiple")
    @Operation(summary = "Uploader plusieurs documents", description = "Téléverse plusieurs fichiers vers le serveur")
    public ResponseEntity<UploadResponse> uploadMultipleDocuments(@RequestParam("files") MultipartFile[] files) {
        try {
            List<FileInfo> uploadedFiles = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        String originalFileName = file.getOriginalFilename();
                        String fileName = UUID.randomUUID().toString() + "_" + originalFileName.replace(" ", "_");

                        Path targetLocation = this.fileStorageLocation.resolve(fileName);
                        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                        uploadedFiles.add(new FileInfo(fileName, originalFileName, file.getSize()));
                    } catch (IOException e) {
                        errors.add("Erreur avec le fichier " + file.getOriginalFilename() + ": " + e.getMessage());
                    }
                }
            }

            String message = uploadedFiles.size() + " fichier(s) uploadé(s) avec succès";
            if (!errors.isEmpty()) {
                message += ". Erreurs: " + String.join(", ", errors);
            }

            UploadResponse response = new UploadResponse(message, uploadedFiles);
            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UploadResponse("Erreur lors de l'upload multiple: " + ex.getMessage(), null));
        }
    }

    // Lister tous les documents
    @GetMapping("/list")
    @Operation(summary = "Lister les documents", description = "Retourne la liste de tous les documents disponibles")
    public ResponseEntity<List<FileInfo>> listDocuments() {
        try {
            List<FileInfo> files = new ArrayList<>();

            Files.list(this.fileStorageLocation)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String originalName = fileName.contains("_") ?
                                    fileName.substring(fileName.indexOf("_") + 1) : fileName;
                            long fileSize = Files.size(path);

                            files.add(new FileInfo(fileName, originalName, fileSize));
                        } catch (IOException e) {
                            // Ignorer les fichiers avec erreur
                        }
                    });

            return ResponseEntity.ok(files);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Pour visualiser dans le navigateur
    @GetMapping("/view/{fileName:.+}")
    @Operation(summary = "Visualiser un document", description = "Affiche le document directement dans le navigateur")
    public ResponseEntity<Resource> viewFile(@PathVariable String fileName) {
        return handleFile(fileName, false);
    }

    // Pour télécharger le fichier
    @GetMapping("/download/{fileName:.+}")
    @Operation(summary = "Télécharger un document", description = "Télécharge le document sur l'appareil de l'utilisateur")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        return handleFile(fileName, true);
    }

    // Supprimer un document
    @DeleteMapping("/{fileName:.+}")
    @Operation(summary = "Supprimer un document", description = "Supprime un document du serveur")
    public ResponseEntity<DeleteResponse> deleteDocument(@PathVariable String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Files.delete(filePath);

            return ResponseEntity.ok(new DeleteResponse("Document supprimé avec succès", fileName));

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DeleteResponse("Erreur lors de la suppression: " + ex.getMessage(), fileName));
        }
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
                headers.setContentDispositionFormData("attachment", getOriginalFileName(fileName));
            } else {
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentDispositionFormData("inline", getOriginalFileName(fileName));
            }

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        switch (extension) {
            case "pdf": return "application/pdf";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "txt": return "text/plain";
            case "html": return "text/html";
            case "doc": return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls": return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default: return "application/octet-stream";
        }
    }

    private String getOriginalFileName(String storedFileName) {
        if (storedFileName.contains("_")) {
            return storedFileName.substring(storedFileName.indexOf("_") + 1);
        }
        return storedFileName;
    }

    // Classes pour les réponses JSON
    public static class UploadResponse {
        private String message;
        private Object files;

        public UploadResponse(String message, Object files) {
            this.message = message;
            this.files = files;
        }

        // Getters et setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Object getFiles() { return files; }
        public void setFiles(Object files) { this.files = files; }
    }

    public static class FileInfo {
        private String storedName;
        private String originalName;
        private long size;
        private String downloadUrl;
        private String viewUrl;

        public FileInfo(String storedName, String originalName, long size) {
            this.storedName = storedName;
            this.originalName = originalName;
            this.size = size;
            this.downloadUrl = "/api/V1/documents/download/" + storedName;
            this.viewUrl = "/api/V1/documents/view/" + storedName;
        }

        // Getters et setters
        public String getStoredName() { return storedName; }
        public String getOriginalName() { return originalName; }
        public long getSize() { return size; }
        public String getDownloadUrl() { return downloadUrl; }
        public String getViewUrl() { return viewUrl; }
    }

    public static class DeleteResponse {
        private String message;
        private String fileName;

        public DeleteResponse(String message, String fileName) {
            this.message = message;
            this.fileName = fileName;
        }

        // Getters et setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}