package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.model.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository chargé de gérer l'accès aux données des exercices comptables.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - créer ou enregistrer un exercice ;
 * - rechercher un exercice par son identifiant ;
 * - récupérer tous les exercices ;
 * - modifier un exercice ;
 * - supprimer un exercice.
 *
 * L'entité gérée est {@link Exercice} et son identifiant principal
 * est de type {@link String}.
 */
public interface ExerciceRepository extends JpaRepository<Exercice, String> {

    /**
     * Recherche un exercice à partir de son année et de son statut.
     *
     * Cette méthode peut notamment servir à retrouver l'exercice actif
     * correspondant à une année donnée.
     *
     * Exemple :
     * année = 2026 et statut = OUVERT.
     *
     * @param annee année de l'exercice recherché
     * @param statut statut de l'exercice recherché
     * @return exercice trouvé dans un Optional, ou Optional.empty()
     *         si aucun exercice ne correspond
     */
    Optional<Exercice> findByAnneeAndStatut(
            Integer annee,
            StatutExercice statut
    );
}