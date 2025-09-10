package com.ogooueTech.smsgateway.config;

import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.model.CalendrierFacturation;
import com.ogooueTech.smsgateway.model.Exercice;
import com.ogooueTech.smsgateway.repository.CalendrierFacturationRepository;
import com.ogooueTech.smsgateway.repository.ExerciceRepository;
import com.ogooueTech.smsgateway.service.FacturationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class FacturationScheduler {

    private final ExerciceRepository exerciceRepository;
    private final CalendrierFacturationRepository calendrierRepository;
    private final FacturationService facturationService;

    public FacturationScheduler(ExerciceRepository exerciceRepository, CalendrierFacturationRepository calendrierRepository, FacturationService facturationService) {
        this.exerciceRepository = exerciceRepository;
        this.calendrierRepository = calendrierRepository;
        this.facturationService = facturationService;
    }

    // Tous les jours à 00:10 (heure serveur)
    @Scheduled(cron = "0 10 0 * * *")
    public void runIfInvoiceDay() {
        LocalDate today = LocalDate.now(); // Timezone: configure si besoin
        int annee = today.getYear();

        Exercice ex = exerciceRepository.findByAnneeAndStatut(annee, StatutExercice.OUVERT)
                .orElse(null);
        if (ex == null) return;

        List<CalendrierFacturation> rows = calendrierRepository.findAll(); // ou une méthode dédiée par exercice
        rows.stream()
                .filter(r -> r.getExercice().getId().equals(ex.getId()))
                .filter(r -> today.equals(r.getDateGenerationFacture()))
                .findFirst()
                .ifPresent(r -> facturationService.genererFacturesMensuelles(annee, r.getMois()));
    }
}
