package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.FooterInfo;
import com.ogooueTech.smsgateway.repository.FooterInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service chargé de gérer les informations légales et commerciales
 * affichées dans le pied de page des factures.
 *
 * Une seule configuration est utilisée dans l'application,
 * avec l'identifiant fixe 1.
 */
@Service
@Transactional
public class FooterInfoService {

    private static final long FOOTER_INFO_ID = 1L;

    private final FooterInfoRepository repository;

    /**
     * Injection du repository.
     *
     * @param repository repository des informations de pied de page
     */
    public FooterInfoService(FooterInfoRepository repository) {
        this.repository = repository;
    }

    /**
     * Retourne les informations du pied de page.
     *
     * Une configuration par défaut est créée lorsque
     * la configuration principale n'existe pas encore.
     *
     * @return informations du pied de page
     */
    public FooterInfo getFooterInfo() {
        return repository.findById(FOOTER_INFO_ID)
                .orElseGet(this::creerInformationsParDefaut);
    }

    /**
     * Met à jour les informations du pied de page.
     *
     * L'identifiant est toujours forcé à 1 afin de conserver
     * une seule configuration globale.
     *
     * @param newInfo nouvelles informations
     * @return informations enregistrées
     */
    public FooterInfo updateFooterInfo(FooterInfo newInfo) {
        if (newInfo == null) {
            throw new IllegalArgumentException(
                    "Les informations du pied de page sont obligatoires"
            );
        }

        newInfo.setId(FOOTER_INFO_ID);

        return repository.save(newInfo);
    }

    /**
     * Crée la configuration par défaut.
     *
     * Les valeurs administratives réelles devront ensuite être
     * renseignées depuis l'interface d'administration.
     */
    private FooterInfo creerInformationsParDefaut() {
        FooterInfo defaultInfo = new FooterInfo();

        defaultInfo.setId(FOOTER_INFO_ID);
        defaultInfo.setCompanyName("OgooueTech S.A.R.L");
        defaultInfo.setCompanyAddress("Libreville, Gabon");
        defaultInfo.setCompanyNif("À RENSEIGNER");
        defaultInfo.setCompanyRccm("À RENSEIGNER");
        defaultInfo.setCompanyEmail("contact@ogoouetech.com");
        defaultInfo.setCompanyPhone("À RENSEIGNER");
        defaultInfo.setPaymentNote(
                "Merci de régler la facture sous 30 jours."
        );

        return repository.save(defaultInfo);
    }
}