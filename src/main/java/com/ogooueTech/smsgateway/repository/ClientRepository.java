package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.Client;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, String> {
    boolean existsByEmail(String email);
    boolean existsByCleApi(String cleApi);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByCleApi(String cleApi); // <-- AJOUT


    Optional<Client> findByIdclients(String idclients);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Client c where c.idclients = :id")
    Optional<Client> lockById(@Param("id") String id);
    List<Client> findByTypeCompteAndStatutCompte(
            TypeCompte typeCompte,
            StatutCompte statutCompte
    );
    List<Client> findByRaisonSocialeContainingIgnoreCase(String raisonSociale);



}


