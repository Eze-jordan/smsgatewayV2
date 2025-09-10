package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.*;
import com.ogooueTech.smsgateway.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service

public class FacturationService {

    private final ExerciceRepository exerciceRepository;
    private final CalendrierFacturationRepository calendrierRepository;
    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;

    public FacturationService(ExerciceRepository exerciceRepository, CalendrierFacturationRepository calendrierRepository, ClientRepository clientRepository, FactureRepository factureRepository) {
        this.exerciceRepository = exerciceRepository;
        this.calendrierRepository = calendrierRepository;
        this.clientRepository = clientRepository;
        this.factureRepository = factureRepository;
    }

    /**
     * Génère les factures pour (annee, mois) en utilisant le prix TTC stocké sur chaque client.
     * Règles:
     *  - Prend uniquement les clients POSTPAYE & ACTIF
     *  - Skip si soldeNet <= 0
     *  - Skip si coutSmsTtc null (client mal paramétré)
     *  - Anti-doublon par (client, dateDebut, dateFin)
     *  - Montant arrondi à 2 décimales (HALF_UP)
     */
    @Transactional
    public BillingRunResult genererFacturesMensuelles(int annee, int mois) {
        Exercice exercice = exerciceRepository
                .findByAnneeAndStatut(annee, StatutExercice.OUVERT)
                .orElseThrow(() -> new IllegalStateException("Exercice " + annee + " non trouvé ou non OUVERT"));

        CalendrierFacturation cal = calendrierRepository
                .findByExerciceAndMois(exercice, mois)
                .orElseThrow(() -> new IllegalStateException("Calendrier absent pour " + annee + "-" + mois));

        LocalDate debut = cal.getDateDebutConsommation();
        LocalDate fin = cal.getDateFinConsommation();

        int created = 0, zero = 0, dup = 0, missingPrice = 0;

        List<Client> clients = clientRepository.findByTypeCompteAndStatutCompte(
                TypeCompte.POSTPAYE, StatutCompte.ACTIF
        );

        for (Client c : clients) {
            int conso = c.getSoldeNet() == null ? 0 : c.getSoldeNet();
            if (conso <= 0) { zero++; continue; }

            BigDecimal prixUnitaireInt = c.getCoutSmsTtc(); // FCFA/SMS entier
            if (prixUnitaireInt == null) { missingPrice++; continue; }

            //Convertir en BigDecimal (FCFA = 0 décimale)
            BigDecimal prixUnitaire = BigDecimal.valueOf(prixUnitaireInt.longValue()); // scale 0
            BigDecimal montant = prixUnitaire
                    .multiply(BigDecimal.valueOf(conso))
                    .setScale(0, RoundingMode.HALF_UP); // cohérent avec FCFA

            boolean dejaCree = factureRepository
                    .findByClientAndDateDebutAndDateFin(c, debut, fin)
                    .isPresent();
            if (dejaCree) { dup++; continue; }

            Facture f = new Facture();
            f.setClient(c);
            f.setExercice(exercice);
            f.setDateDebut(debut);
            f.setDateFin(fin);
            f.setConsommationSms(conso);
            f.setPrixUnitaire(prixUnitaire); // BigDecimal
            f.setMontant(montant);           // BigDecimal

            factureRepository.save(f);
            // ✅ POSTPAYE : mémoriser la conso du cycle
            c.setLastSoldeNet(conso);

            c.setSoldeNet(0);
            clientRepository.save(c);

            created++;
        }

        return new BillingRunResult(created, zero, dup, missingPrice);
    }

    /**
     * Récupère toutes les factures en base
     */
    @Transactional(readOnly = true)
    public List<Facture> getAllFactures() {
        return factureRepository.findAll();
    }
    // Petit DTO de résumé (mets-le dans un fichier séparé si tu préfères)
    public record BillingRunResult(int generated, int skippedZero, int skippedDuplicate, int skippedMissingPrice) {}
}
