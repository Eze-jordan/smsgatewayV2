package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.CalendrierFacturation;
import com.ogooueTech.smsgateway.model.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CalendrierFacturationRepository extends JpaRepository<CalendrierFacturation, String> {
    List<CalendrierFacturation> findByExerciceOrderByMoisAsc(Exercice exercice);
    Optional<CalendrierFacturation> findByExerciceAndMois(Exercice exercice, Integer mois);

}