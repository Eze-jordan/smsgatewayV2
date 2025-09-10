package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "sms_recipients")
public class SmsRecipient {
    @Id
    private String id;

    @ManyToOne(optional = false) @JoinColumn(name = "ref_sms")
    private SmsMessage sms;

    @Column(length = 20, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    private SmsStatus statut = SmsStatus.EN_ATTENTE;

    private String lastError; // si ECHEC

    @Column(updatable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // Générateur ref 6 chiffres
    private static final java.util.concurrent.atomic.AtomicLong LAST_REF =
            new java.util.concurrent.atomic.AtomicLong(100000);
    private String generateRef() {
        return String.format("%06d", LAST_REF.incrementAndGet());
    }
    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = generateRef();
        }
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
