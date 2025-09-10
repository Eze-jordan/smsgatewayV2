package com.ogooueTech.smsgateway.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "calendriers_facturation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_calendrier_exercice_mois", columnNames = {"exercice_id", "mois"})
})
@Getter @Setter
public class CalendrierFacturation {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    // 1..12
    @Column(nullable = false)
    private Integer mois;

    @Column(nullable = false)
    private LocalDate dateDebutConsommation;

    @Column(nullable = false)
    private LocalDate dateFinConsommation;

    @Column(nullable = false)
    private LocalDate dateGenerationFacture;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "exercice_id", nullable = false)
    private Exercice exercice;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getMois() {
        return mois;
    }

    public void setMois(Integer mois) {
        this.mois = mois;
    }

    public LocalDate getDateDebutConsommation() {
        return dateDebutConsommation;
    }

    public void setDateDebutConsommation(LocalDate dateDebutConsommation) {
        this.dateDebutConsommation = dateDebutConsommation;
    }

    public LocalDate getDateFinConsommation() {
        return dateFinConsommation;
    }

    public void setDateFinConsommation(LocalDate dateFinConsommation) {
        this.dateFinConsommation = dateFinConsommation;
    }

    public LocalDate getDateGenerationFacture() {
        return dateGenerationFacture;
    }

    public void setDateGenerationFacture(LocalDate dateGenerationFacture) {
        this.dateGenerationFacture = dateGenerationFacture;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
    }
}
