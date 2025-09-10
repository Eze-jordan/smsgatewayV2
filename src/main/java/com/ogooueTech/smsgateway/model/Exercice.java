// com.ogooueTech.smsgateway.model.Exercice.java
package com.ogooueTech.smsgateway.model;

import com.ogooueTech.smsgateway.enums.StatutExercice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exercices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exercice_annee", columnNames = {"annee"})
})
@Getter @Setter
public class Exercice {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private Integer annee; // ex: 2026

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutExercice statut = StatutExercice.OUVERT;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public StatutExercice getStatut() {
        return statut;
    }

    public void setStatut(StatutExercice statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
