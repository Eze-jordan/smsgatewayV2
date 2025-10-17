package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.enums.SmsType;
import com.ogooueTech.smsgateway.model.SmsMessage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PendingSmsDetailsDTO {
    private String ref;
    private SmsType type;
    private String clientId;
    private String emetteur;
    private String corps;
    private SmsStatus statut;
    private LocalDateTime createdAt;
    private List<String> recipients;
    private int totalRecipients;

    // Informations spécifiques aux MULDESP
    private LocalDate dateDebutEnvoi;
    private LocalDate dateFinEnvoi;
    private Integer nbParJour;
    private Integer nbDejaEnvoye;
    private Integer intervalleMinutes;

    // Constructeurs
    public PendingSmsDetailsDTO() {}

    public PendingSmsDetailsDTO(SmsMessage sms, List<String> recipients) {
        this.ref = sms.getRef();
        this.type = sms.getType();
        this.clientId = sms.getClientId();
        this.emetteur = sms.getEmetteur();
        this.corps = sms.getCorps();
        this.statut = sms.getStatut();
        this.createdAt = sms.getCreatedAt();
        this.recipients = recipients;
        this.totalRecipients = recipients.size();

        // Champs spécifiques MULDESP
        this.dateDebutEnvoi = sms.getDateDebutEnvoi();
        this.dateFinEnvoi = sms.getDateFinEnvoi();
        this.nbParJour = sms.getNbParJour();
        this.nbDejaEnvoye = sms.getNbDejaEnvoye();
        this.intervalleMinutes = sms.getIntervalleMinutes();
    }

    // Getters et Setters
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public SmsType getType() { return type; }
    public void setType(SmsType type) { this.type = type; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getEmetteur() { return emetteur; }
    public void setEmetteur(String emetteur) { this.emetteur = emetteur; }

    public String getCorps() { return corps; }
    public void setCorps(String corps) { this.corps = corps; }

    public SmsStatus getStatut() { return statut; }
    public void setStatut(SmsStatus statut) { this.statut = statut; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
        this.totalRecipients = recipients != null ? recipients.size() : 0;
    }

    public int getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(int totalRecipients) { this.totalRecipients = totalRecipients; }

    public LocalDate getDateDebutEnvoi() { return dateDebutEnvoi; }
    public void setDateDebutEnvoi(LocalDate dateDebutEnvoi) { this.dateDebutEnvoi = dateDebutEnvoi; }

    public LocalDate getDateFinEnvoi() { return dateFinEnvoi; }
    public void setDateFinEnvoi(LocalDate dateFinEnvoi) { this.dateFinEnvoi = dateFinEnvoi; }

    public Integer getNbParJour() { return nbParJour; }
    public void setNbParJour(Integer nbParJour) { this.nbParJour = nbParJour; }

    public Integer getNbDejaEnvoye() { return nbDejaEnvoye; }
    public void setNbDejaEnvoye(Integer nbDejaEnvoye) { this.nbDejaEnvoye = nbDejaEnvoye; }

    public Integer getIntervalleMinutes() { return intervalleMinutes; }
    public void setIntervalleMinutes(Integer intervalleMinutes) { this.intervalleMinutes = intervalleMinutes; }
}