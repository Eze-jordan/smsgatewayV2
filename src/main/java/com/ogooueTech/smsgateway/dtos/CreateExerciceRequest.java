package com.ogooueTech.smsgateway.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateExerciceRequest {

    @NotNull
    private Integer annee;            // ex: 2026

    // Jour de génération des factures (dans le MOIS SUIVANT) pour chaque période mensuelle
    // 1..28 pour éviter les soucis de février; défaut = 1er du mois suivant
    @Min(1) @Max(28)
    private Integer invoiceDayOfNextMonth = 1;

    // Si true et si un exercice OUVERT existe déjà pour cette année : on régénère le calendrier.
    // Si false : on lève une exception si déjà présent.
    private Boolean overwriteIfExists = false;

    public @NotNull Integer getAnnee() {
        return annee;
    }

    public void setAnnee(@NotNull Integer annee) {
        this.annee = annee;
    }

    public @Min(1) @Max(28) Integer getInvoiceDayOfNextMonth() {
        return invoiceDayOfNextMonth;
    }

    public void setInvoiceDayOfNextMonth(@Min(1) @Max(28) Integer invoiceDayOfNextMonth) {
        this.invoiceDayOfNextMonth = invoiceDayOfNextMonth;
    }

    public Boolean getOverwriteIfExists() {
        return overwriteIfExists;
    }

    public void setOverwriteIfExists(Boolean overwriteIfExists) {
        this.overwriteIfExists = overwriteIfExists;
    }
}
