package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_messages")
public class SmsMessage {
    @Id
    private String ref; // 6 chiffres, générés côté service (ex: "700123")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsType type;

    // UNIDES: un seul destinataire
    // MULDES / MULDESP: soit on stocke la liste dans une table jointe,
    // soit on garde un "groupe" (id) et on résout dans le service.
    @Column(length = 20) // E.164 ~ 15, on met un peu de marge
    private String destinataire; // optionnel si multi

    @Column(nullable = false, length = 160)
    private String corps;

    @Column(nullable = false, length = 11) // Alphanumeric Sender ID (jusqu'à 11)
    private String emetteur;

    @Column(nullable = false)
    private String clientId;

    // Planification (utilisé surtout pour MULDESP)
    private java.time.LocalDateTime dateDebutEnvoi;
    private Integer nbParJour;            // Nombre d’envoi de SMS par jour
    private Integer nbDejaEnvoye;         // Compteur
    private java.time.LocalDateTime prochaineHeureEnvoi;
    private Integer intervalleMinutes;    // Intervalle entre 2 envois
    private java.time.LocalDateTime dateFinEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SmsStatus statut = SmsStatus.EN_ATTENTE;

    // Audit
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
        if (nbDejaEnvoye == null) nbDejaEnvoye = 0;
    }
    @PreUpdate
    void preUpdate() { updatedAt = java.time.LocalDateTime.now(); }


    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public SmsType getType() {
        return type;
    }

    public void setType(SmsType type) {
        this.type = type;
    }

    public String getDestinataire() {
        return destinataire;
    }

    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }

    public String getCorps() {
        return corps;
    }

    public void setCorps(String corps) {
        this.corps = corps;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getDateDebutEnvoi() {
        return dateDebutEnvoi;
    }

    public void setDateDebutEnvoi(LocalDateTime dateDebutEnvoi) {
        this.dateDebutEnvoi = dateDebutEnvoi;
    }

    public Integer getNbParJour() {
        return nbParJour;
    }

    public void setNbParJour(Integer nbParJour) {
        this.nbParJour = nbParJour;
    }

    public Integer getNbDejaEnvoye() {
        return nbDejaEnvoye;
    }

    public void setNbDejaEnvoye(Integer nbDejaEnvoye) {
        this.nbDejaEnvoye = nbDejaEnvoye;
    }

    public LocalDateTime getProchaineHeureEnvoi() {
        return prochaineHeureEnvoi;
    }

    public void setProchaineHeureEnvoi(LocalDateTime prochaineHeureEnvoi) {
        this.prochaineHeureEnvoi = prochaineHeureEnvoi;
    }

    public Integer getIntervalleMinutes() {
        return intervalleMinutes;
    }

    public void setIntervalleMinutes(Integer intervalleMinutes) {
        this.intervalleMinutes = intervalleMinutes;
    }

    public LocalDateTime getDateFinEnvoi() {
        return dateFinEnvoi;
    }

    public void setDateFinEnvoi(LocalDateTime dateFinEnvoi) {
        this.dateFinEnvoi = dateFinEnvoi;
    }

    public SmsStatus getStatut() {
        return statut;
    }

    public void setStatut(SmsStatus statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
