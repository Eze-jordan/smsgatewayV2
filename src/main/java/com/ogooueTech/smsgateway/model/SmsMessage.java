package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sms_messages")
public class SmsMessage {
    @Id
    private String ref; // 6 chiffres, générés côté service (ex: "700123")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsType type;

    @Column(length = 20) // E.164 ~ 15
    private String destinataire;

    @Column(nullable = false, length = 160)
    private String corps;

    @Column(nullable = false, length = 11)
    private String emetteur;

    @Column(nullable = false)
    private String clientId;

    // Planification simplifiée
    private LocalDate dateDebutEnvoi;
    private LocalDate dateFinEnvoi;
    private Integer nbParJour;            // Nombre d’envois par jour
    private Integer intervalleMinutes;    // Intervalle entre 2 envois
    private Integer nbDejaEnvoye;         // Compteur

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsStatus statut = SmsStatus.EN_ATTENTE;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (nbDejaEnvoye == null) nbDejaEnvoye = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ====== Getters / Setters ======
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public SmsType getType() { return type; }
    public void setType(SmsType type) { this.type = type; }

    public String getDestinataire() { return destinataire; }
    public void setDestinataire(String destinataire) { this.destinataire = destinataire; }

    public String getCorps() { return corps; }
    public void setCorps(String corps) { this.corps = corps; }

    public String getEmetteur() { return emetteur; }
    public void setEmetteur(String emetteur) { this.emetteur = emetteur; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public LocalDate getDateDebutEnvoi() {
        return dateDebutEnvoi;
    }

    public void setDateDebutEnvoi(LocalDate dateDebutEnvoi) {
        this.dateDebutEnvoi = dateDebutEnvoi;
    }

    public LocalDate getDateFinEnvoi() {
        return dateFinEnvoi;
    }

    public void setDateFinEnvoi(LocalDate dateFinEnvoi) {
        this.dateFinEnvoi = dateFinEnvoi;
    }

    public Integer getNbParJour() { return nbParJour; }
    public void setNbParJour(Integer nbParJour) { this.nbParJour = nbParJour; }

    public Integer getIntervalleMinutes() { return intervalleMinutes; }
    public void setIntervalleMinutes(Integer intervalleMinutes) { this.intervalleMinutes = intervalleMinutes; }

    public Integer getNbDejaEnvoye() { return nbDejaEnvoye; }
    public void setNbDejaEnvoye(Integer nbDejaEnvoye) { this.nbDejaEnvoye = nbDejaEnvoye; }

    public SmsStatus getStatut() { return statut; }
    public void setStatut(SmsStatus statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
