package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Emetteur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmetteurRepository extends JpaRepository<Emetteur, String> {
    List<Emetteur> findByClient_Idclients(String clientId);
    boolean existsByClient_IdclientsAndNomIgnoreCase(String clientId, String nom);

}
