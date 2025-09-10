package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutExercice;
import com.ogooueTech.smsgateway.model.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExerciceRepository extends JpaRepository<Exercice, String> {
    Optional<Exercice> findByAnneeAndStatut(Integer annee, StatutExercice statut);
}
