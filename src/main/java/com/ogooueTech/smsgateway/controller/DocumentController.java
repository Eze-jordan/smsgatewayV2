    package com.ogooueTech.smsgateway.controller;

    import com.ogooueTech.smsgateway.model.Document;
    import com.ogooueTech.smsgateway.service.DocumentService;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import org.springframework.core.io.Resource;
    import org.springframework.http.*;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;

    import java.util.List;

    @RestController
    @RequestMapping("/api/V1/documents")
    @Tag(name = "Gestion des Documents")
    public class DocumentController {

        private final DocumentService documentService;

        public DocumentController(DocumentService documentService) {
            this.documentService = documentService;
        }

        @PostMapping("/upload")
        @Operation(summary = "Uploader un document")
        public ResponseEntity<Document> upload(@RequestParam("file") MultipartFile file) {
            try {
                return ResponseEntity.ok(documentService.upload(file));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        @GetMapping("/list")
        @Operation(summary = "Lister tous les documents")
        public List<Document> list() {
            return documentService.listAll();
        }

        @GetMapping("/view/{fileName:.+}")
        @Operation(summary = "Visualiser un document")
        public ResponseEntity<Resource> view(@PathVariable String fileName) {
            try {
                Resource resource = documentService.load(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }

        @GetMapping("/download/{fileName:.+}")
        @Operation(summary = "Télécharger un document")
        public ResponseEntity<Resource> download(@PathVariable String fileName) {
            try {
                Resource resource = documentService.load(fileName);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } catch (Exception e) {
                return ResponseEntity.notFound().build();
            }
        }

        @DeleteMapping("/{fileName:.+}")
        @Operation(summary = "Supprimer un document")
        public ResponseEntity<Void> delete(@PathVariable String fileName) {
            try {
                documentService.delete(fileName);
                return ResponseEntity.noContent().build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
