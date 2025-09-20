package com.ogooueTech.smsgateway.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "emetteurs")
@Getter
@Setter
public class Emetteur {

    @Id
    @GeneratedValue
    @UuidGenerator
    private String id;

    @Column(nullable = false, length = 11)
    private String nom; // alias expéditeur (max 11 caractères pour SMS)

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id")
    private Client client; // chaque émetteur appartient à un client

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt; // date de création auto

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
