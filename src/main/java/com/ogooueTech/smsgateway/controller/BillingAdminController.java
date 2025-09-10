package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.dtos.CreateExerciceRequest;
import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.model.CalendrierFacturation;
import com.ogooueTech.smsgateway.model.Exercice;
import com.ogooueTech.smsgateway.repository.CalendrierFacturationRepository;
import com.ogooueTech.smsgateway.repository.ExerciceRepository;
import com.ogooueTech.smsgateway.service.FacturationService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Swagger OpenAPI
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/V1/billing")
@Tag(name = "Billing", description = "Administration endpoints for billing (fiscal years & invoice generation)")
public class BillingAdminController {

    private final ExerciceRepository exerciceRepository;
    private final CalendrierFacturationRepository calendrierRepository;
    private final FacturationService facturationService;

    // Default value if no price is provided
    @Value("${app.billing.sms-price:0.020}")
    private BigDecimal defaultSmsPrice;

    public BillingAdminController(ExerciceRepository exerciceRepository,
                                  CalendrierFacturationRepository calendrierRepository,
                                  FacturationService facturationService) {
        this.exerciceRepository = exerciceRepository;
        this.calendrierRepository = calendrierRepository;
        this.facturationService = facturationService;
    }

    /**
     * Create a fiscal year (status OPEN) and generate the 12 billing calendar rows.
     * For each month m:
     *  - startDate = 1st day of month m
     *  - endDate   = last day of month m
     *  - invoiceDate = 'invoiceDayOfNextMonth' day of month (m+1)
     */
    @PostMapping("/exercices")
    @Transactional
    @Operation(summary = "Create a fiscal year and its billing calendar", tags = "Billing")
    public ResponseEntity<List<CalendrierFacturation>> createExerciceWithCalendar(
            @Valid @RequestBody CreateExerciceRequest req
    ) {
        int year = req.getAnnee();
        int invoiceDay = req.getInvoiceDayOfNextMonth() == null ? 1 : req.getInvoiceDayOfNextMonth();

        Exercice existing = exerciceRepository.findByAnneeAndStatut(year, StatutExercice.OUVERT).orElse(null);

        if (existing != null && Boolean.TRUE.equals(req.getOverwriteIfExists())) {
            // purge existing calendar
            calendrierRepository.findByExerciceOrderByMoisAsc(existing)
                    .forEach(calendrierRepository::delete);
            // rebuild
            buildCalendarForYear(existing, invoiceDay);
            var rows = calendrierRepository.findByExerciceOrderByMoisAsc(existing);
            return ResponseEntity.ok(rows);
        }

        if (existing != null) {
            throw new IllegalStateException("An OPEN fiscal year already exists for " + year +
                    " (use overwriteIfExists=true to regenerate the calendar).");
        }

        Exercice ex = new Exercice();
        ex.setAnnee(year);
        ex.setStatut(StatutExercice.OUVERT);
        ex = exerciceRepository.save(ex);

        buildCalendarForYear(ex, invoiceDay);

        var rows = calendrierRepository.findByExerciceOrderByMoisAsc(ex);
        return ResponseEntity.ok(rows);
    }

    /**
     * Trigger invoice generation for (year, month).
     * If smsPrice is omitted -> default value app.billing.sms-price is used.
     */
    @PostMapping("/generer")
    @Operation(summary = "Generate monthly invoices", tags = "Billing")
    public ResponseEntity<?> genererFactures(
            @RequestParam int annee,
            @RequestParam int mois
    ) {
        var result = facturationService.genererFacturesMensuelles(annee, mois);
        return ResponseEntity.ok(result);
    }

    /**
     * View the billing calendar of an open fiscal year.
     */
    @GetMapping("/exercices/{annee}/calendrier")
    @Operation(summary = "View the billing calendar of a fiscal year", tags = "Billing")
    public ResponseEntity<List<CalendrierFacturation>> getCalendrier(@PathVariable int annee) {
        var ex = exerciceRepository.findByAnneeAndStatut(annee, StatutExercice.OUVERT)
                .orElseThrow(() -> new IllegalStateException("Fiscal year " + annee + " not found or not OPEN"));
        var all = calendrierRepository.findByExerciceOrderByMoisAsc(ex);
        return ResponseEntity.ok(all);
    }

    /* ----------------------- Helpers ----------------------- */

    private void buildCalendarForYear(Exercice ex, int invoiceDayOfNextMonth) {
        int year = ex.getAnnee();
        for (int m = 1; m <= 12; m++) {
            LocalDate start = LocalDate.of(year, m, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            LocalDate monthAfter = end.plusMonths(1).withDayOfMonth(1);
            int maxDayNextMonth = monthAfter.lengthOfMonth();
            int safeDay = Math.min(invoiceDayOfNextMonth, maxDayNextMonth);
            LocalDate invoiceDate = monthAfter.withDayOfMonth(safeDay);

            CalendrierFacturation row = new CalendrierFacturation();
            row.setExercice(ex);
            row.setMois(m);
            row.setDateDebutConsommation(start);
            row.setDateFinConsommation(end);
            row.setDateGenerationFacture(invoiceDate);

            calendrierRepository.save(row);
        }
    }
}
