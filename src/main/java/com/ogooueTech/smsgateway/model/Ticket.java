package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.StatutTicket;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String clientId;       // identifiant client
    private String emailClient;    // email du client
    private String titre;          // objet du ticket
    @Column(length = 2000)
    private String description;    // détail du problème

    @Enumerated(EnumType.STRING)
    private StatutTicket statut = StatutTicket.OUVERT; // par défaut

    @Column(length = 2000)
    private String reponseAdmin;   // réponse de l’admin

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getEmailClient() { return emailClient; }
    public void setEmailClient(String emailClient) { this.emailClient = emailClient; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StatutTicket getStatut() { return statut; }
    public void setStatut(StatutTicket statut) { this.statut = statut; }

    public String getReponseAdmin() { return reponseAdmin; }
    public void setReponseAdmin(String reponseAdmin) { this.reponseAdmin = reponseAdmin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
