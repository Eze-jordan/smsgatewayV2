package com.ogooueTech.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
@Entity
@Table(
        name = "clients_groups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_client_nomgroupe",
                        columnNames = {"client_id", "nom_groupe"}
                )
        }
)
public class ClientsGroups {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(length = 36)
    private String idClientsGroups;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore  // <- ne pas sérialiser le client dans la réponse du groupe
    private Client client;

    @Column(name = "nom_groupe", nullable = false, length = 150)
    private String nomGroupe;

    @Column(name = "description_groupe", length = 500)
    private String descriptionGroupe;

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();


    // --- Getters / Setters ---

    public String getIdClientsGroups() {
        return idClientsGroups;
    }

    public void setIdClientsGroups(String idContactGroup) {
        this.idClientsGroups = idContactGroup;
    }

    public String getNomGroupe() {
        return nomGroupe;
    }

    public void setNomGroupe(String nomGroupe) {
        this.nomGroupe = nomGroupe;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescriptionGroupe() {
        return descriptionGroupe;
    }

    public void setDescriptionGroupe(String descriptionGroupe) {
        this.descriptionGroupe = descriptionGroupe;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
