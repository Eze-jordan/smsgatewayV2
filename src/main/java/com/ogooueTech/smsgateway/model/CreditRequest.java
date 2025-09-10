package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.CreditStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "credit_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_credit_req_idempotency",
                columnNames = "idempotency_key"
        )
)
public class CreditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, length = 36)
    private UUID id;

    // ✅ évite String: on référence directement l’entité Client
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Integer quantity; // nombre de SMS à créditer

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CreditStatus status = CreditStatus.PENDING;

    @Column(name = "maker_email", nullable = false, length = 120)
    private String makerEmail;

    @Column(name = "checker_email", length = 120)
    private String checkerEmail;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    private LocalDateTime createdAt;
    private LocalDateTime validatedAt;

    @PrePersist
    void prePersist() { createdAt = LocalDateTime.now(); }

    // getters/setters
    public UUID getId() { return id; }

    public void setId(UUID id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public CreditStatus getStatus() { return status; }
    public void setStatus(CreditStatus status) { this.status = status; }

    public String getMakerEmail() { return makerEmail; }
    public void setMakerEmail(String makerEmail) { this.makerEmail = makerEmail; }

    public String getCheckerEmail() { return checkerEmail; }
    public void setCheckerEmail(String checkerEmail) { this.checkerEmail = checkerEmail; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}
