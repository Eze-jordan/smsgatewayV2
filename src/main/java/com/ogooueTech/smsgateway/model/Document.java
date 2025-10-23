package com.ogooueTech.smsgateway.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storedName;
    private String originalName;
    private long size;
    private String contentType;
    private LocalDateTime uploadedAt;

    public Document() {}

    public Document(String storedName, String originalName, long size, String contentType) {
        this.storedName = storedName;
        this.originalName = originalName;
        this.size = size;
        this.contentType = contentType;
        this.uploadedAt = LocalDateTime.now();
    }

    // ========== GETTERS & SETTERS ==========
    public Long getId() { return id; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    // ========== URLS pour le frontend ==========
    @Transient
    public String getDownloadUrl() {
        return "/api/V1/documents/download/" + originalName;
    }

    @Transient
    public String getViewUrl() {
        return "/api/V1/documents/view/" + originalName;
    }
}
