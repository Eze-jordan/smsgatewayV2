package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.FactureDTO;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.CalendrierFacturation;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Exercice;
import com.ogooueTech.smsgateway.model.Facture;
import com.ogooueTech.smsgateway.repository.CalendrierFacturationRepository;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.ExerciceRepository;
import com.ogooueTech.smsgateway.repository.FactureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class FacturationService {

    private final InvoiceAppService invoiceAppService; // ✅ ajout
    private final ExerciceRepository exerciceRepository;
    private final CalendrierFacturationRepository calendrierRepository;
    private final ClientRepository clientRepository;
    private final FactureRepository factureRepository;

    public FacturationService(
            InvoiceAppService invoiceAppService,
            ExerciceRepository exerciceRepository,
            CalendrierFacturationRepository calendrierRepository,
            ClientRepository clientRepository,
            FactureRepository factureRepository
    ) {
        this.invoiceAppService = invoiceAppService;
        this.exerciceRepository = exerciceRepository;
        this.calendrierRepository = calendrierRepository;
        this.clientRepository = clientRepository;
        this.factureRepository = factureRepository;
    }

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

            BigDecimal prixUnitaireInt = c.getCoutSmsTtc();
            if (prixUnitaireInt == null) { missingPrice++; continue; }

            BigDecimal prixUnitaire = BigDecimal.valueOf(prixUnitaireInt.longValue());
            BigDecimal montant = prixUnitaire
                    .multiply(BigDecimal.valueOf(conso))
                    .setScale(0, RoundingMode.HALF_UP);

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
            f.setPrixUnitaire(prixUnitaire);
            f.setMontant(montant);

            // ✅ on sauvegarde et récupère la facture persistée
            Facture saved = factureRepository.save(f);

            // ✅ Envoi automatique par mail après création
            try {
                invoiceAppService.sendPdfByEmail(saved.getId());
            } catch (Exception e) {
                System.err.println("❌ Envoi de facture échoué pour " + c.getEmail() + " : " + e.getMessage());
            }

            c.setLastSoldeNet(conso);
            c.setSoldeNet(0);
            clientRepository.save(c);

            created++;
        }

        return new BillingRunResult(created, zero, dup, missingPrice);
    }

    @Transactional
    public BillingRunResult genererFactureMensuellePourClient(String clientId, int annee, int mois) {
        Exercice exercice = exerciceRepository
                .findByAnneeAndStatut(annee, StatutExercice.OUVERT)
                .orElseThrow(() -> new IllegalStateException("Exercice " + annee + " non trouvé ou non OUVERT"));

        CalendrierFacturation cal = calendrierRepository
                .findByExerciceAndMois(exercice, mois)
                .orElseThrow(() -> new IllegalStateException("Calendrier absent pour " + annee + "-" + mois));

        LocalDate debut = cal.getDateDebutConsommation();
        LocalDate fin = cal.getDateFinConsommation();

        Client c = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable : " + clientId));

        if (c.getTypeCompte() != TypeCompte.POSTPAYE)
            throw new IllegalStateException("Le client n'est pas en POSTPAYE.");
        if (c.getStatutCompte() != StatutCompte.ACTIF)
            throw new IllegalStateException("Le compte du client est inactif ou suspendu.");

        int conso = c.getSoldeNet() == null ? 0 : c.getSoldeNet();
        if (conso <= 0)
            return new BillingRunResult(0, 1, 0, 0);

        BigDecimal prixUnitaireInt = c.getCoutSmsTtc();
        if (prixUnitaireInt == null)
            return new BillingRunResult(0, 0, 0, 1);

        boolean dejaCree = factureRepository
                .findByClientAndDateDebutAndDateFin(c, debut, fin)
                .isPresent();
        if (dejaCree)
            return new BillingRunResult(0, 0, 1, 0);

        BigDecimal prixUnitaire = BigDecimal.valueOf(prixUnitaireInt.longValue());
        BigDecimal montant = prixUnitaire
                .multiply(BigDecimal.valueOf(conso))
                .setScale(0, RoundingMode.HALF_UP);

        Facture f = new Facture();
        f.setClient(c);
        f.setExercice(exercice);
        f.setDateDebut(debut);
        f.setDateFin(fin);
        f.setConsommationSms(conso);
        f.setPrixUnitaire(prixUnitaire);
        f.setMontant(montant);

        Facture saved = factureRepository.save(f);

        try {
            invoiceAppService.sendPdfByEmail(saved.getId());
        } catch (Exception e) {
            System.err.println("❌ Envoi facture client " + c.getEmail() + " : " + e.getMessage());
        }

        c.setLastSoldeNet(conso);
        c.setSoldeNet(0);
        clientRepository.save(c);

        return new BillingRunResult(1, 0, 0, 0);
    }

    @Transactional(readOnly = true)
    public List<Facture> getAllFactures() {
        return factureRepository.findAll();
    }

    public record BillingRunResult(int generated, int skippedZero, int skippedDuplicate, int skippedMissingPrice) {}

    @Transactional(readOnly = true)
    public List<FactureDTO> getFacturesByClient(String clientId) {
        return factureRepository.findByClient_Idclients(clientId)
                .stream()
                .map(FactureDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FactureDTO> getFacturesByClientAndDateRange(String clientId, LocalDate start, LocalDate end) {
        return factureRepository.findByClient_IdclientsAndDateDebutGreaterThanEqualAndDateFinLessThanEqual(
                        clientId, start, end
                ).stream()
                .map(FactureDTO::from)
                .toList();
    }
}
