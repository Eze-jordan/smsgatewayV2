package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, String> {
    Optional<Facture> findByClientAndDateDebutAndDateFin(Client client, LocalDate dateDebut, LocalDate dateFin);
    @Query("""
      select f from Facture f
      join fetch f.client c
      join fetch f.exercice e
      where f.id = :id
    """)
    Optional<Facture> findDetailById(String id);
    List<Facture> findByClient_Idclients(String clientId);
    List<Facture> findByClient_IdclientsAndDateDebutGreaterThanEqualAndDateFinLessThanEqual(
            String clientId, LocalDate start, LocalDate end
    );


}