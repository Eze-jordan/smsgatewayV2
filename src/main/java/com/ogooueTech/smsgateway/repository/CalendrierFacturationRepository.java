package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.CalendrierFacturation;
import com.ogooueTech.smsgateway.model.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository chargé de gérer l'accès aux données
 * des calendriers de facturation.
 *
 * JpaRepository fournit automatiquement les opérations CRUD :
 * - enregistrer un calendrier de facturation ;
 * - rechercher un calendrier par son identifiant ;
 * - récupérer tous les calendriers ;
 * - modifier un calendrier ;
 * - supprimer un calendrier.
 *
 * L'entité gérée est CalendrierFacturation et son identifiant
 * principal est de type String.
 */
public interface CalendrierFacturationRepository
        extends JpaRepository<CalendrierFacturation, String> {

    /**
     * Recherche tous les calendriers de facturation
     * associés à un exercice donné.
     *
     * Les résultats sont triés par mois dans l'ordre croissant,
     * de janvier à décembre.
     *
     * @param exercice exercice comptable ou année de facturation recherchée
     * @return liste des calendriers de facturation triés par mois croissant
     */
    List<CalendrierFacturation> findByExerciceOrderByMoisAsc(
            Exercice exercice
    );

    /**
     * Recherche un calendrier de facturation précis
     * à partir de l'exercice et du numéro du mois.
     *
     * Exemple :
     * exercice 2026 et mois 7 correspondent à juillet 2026.
     *
     * Optional est utilisé car aucun calendrier ne peut exister
     * pour la combinaison exercice et mois demandée.
     *
     * @param exercice exercice comptable ou année de facturation
     * @param mois numéro du mois, généralement compris entre 1 et 12
     * @return calendrier trouvé dans un Optional, ou Optional.empty()
     *         si aucun calendrier ne correspond
     */
    Optional<CalendrierFacturation> findByExerciceAndMois(
            Exercice exercice,
            Integer mois
    );
}