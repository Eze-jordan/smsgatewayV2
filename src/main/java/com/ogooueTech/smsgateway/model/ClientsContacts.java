package com.ogooueTech.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;

@Entity
@Table(
        name = "client_contacts",
        indexes = {
                @Index(name = "ix_contacts_group", columnList = "group_id"),
                @Index(name = "ix_contacts_client", columnList = "client_id")
        }
)
public class ClientsContacts {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id_clients_contacts", nullable = false, length = 36, updatable = false)
    private String idClientsContacts;

    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ClientsGroups clientsGroup;


    // 🔴 dénormalisé: pour l'unicité (client_id, lower(contact_number))
    @Column(name = "client_id", nullable = false, length = 36)
    private String clientId;

    @Column(name = "contact_number", nullable = false, length = 32)
    private String contactNumber;

    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = createdAt;
        // sync clientId depuis le groupe
        if (clientsGroup != null && clientsGroup.getClient() != null) {
            this.clientId = clientsGroup.getClient().getIdclients();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (clientsGroup != null && clientsGroup.getClient() != null) {
            this.clientId = clientsGroup.getClient().getIdclients();
        }
    }

    // Getters / Setters
    public String getIdClientsContacts() { return idClientsContacts; }
    public void setIdClientsContacts(String id) { this.idClientsContacts = id; }

    public ClientsGroups getClientsGroup() { return clientsGroup; }
    public void setClientsGroup(ClientsGroups g) { this.clientsGroup = g; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String n) { this.contactNumber = n; }

    public String getContactName() { return contactName; }
    public void setContactName(String n) { this.contactName = n; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
