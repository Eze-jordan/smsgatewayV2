package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sms_recipients")
public class SmsRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20, nullable = false)
    private String refDestinataire;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ref_sms")
    private SmsMessage sms;

    @Column(length = 12, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    private SmsStatus statut = SmsStatus.EN_ATTENTE;

    private String lastError;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (this.refDestinataire == null) {
            // Exemple de ref courte : R + 4 derniers chiffres du timestamp + 3 chiffres aléatoires
            long millis = System.currentTimeMillis() % 100000000L; // garde 8 derniers chiffres du temps
            int random = (int) (Math.random() * 900) + 100; // nombre aléatoire sur 3 chiffres
            this.refDestinataire = "R" + millis + random; // ex: R8456234112
        }
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }


    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRefDestinataire() {
        return refDestinataire;
    }

    public void setRefDestinataire(String refDestinataire) {
        this.refDestinataire = refDestinataire;
    }

    public SmsMessage getSms() {
        return sms;
    }

    public void setSms(SmsMessage sms) {
        this.sms = sms;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public SmsStatus getStatut() {
        return statut;
    }

    public void setStatut(SmsStatus statut) {
        this.statut = statut;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
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
}
