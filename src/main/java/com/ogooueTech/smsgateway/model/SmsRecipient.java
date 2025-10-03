package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "sms_recipients")
public class SmsRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // clé technique DB

    @Column(unique = true, length = 6, nullable = false)
    private String refDestinataire; // ex: "100001"

    @ManyToOne(optional = false)
    @JoinColumn(name = "ref_sms")
    private SmsMessage sms;

    @Column(length = 20, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    private SmsStatus statut = SmsStatus.EN_ATTENTE;

    private String lastError; // si ECHEC

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    // Générateur de référence sur 6 chiffres
    private static final AtomicLong LAST_REF = new AtomicLong(100000);

    @PrePersist
    void prePersist() {
        if (this.refDestinataire == null) {
            this.refDestinataire = String.format("%06d", LAST_REF.incrementAndGet());
        }
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters / Setters
    public Long getId() {
        return id;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
