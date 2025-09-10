package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.ClientsGroups;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientsGroupsRepository extends JpaRepository<ClientsGroups, String> {

    // Pour lister les groupes d’un client (tu l’as déjà)
    java.util.List<ClientsGroups> findAllByClient_Idclients(String clientId);

    // Un nom déjà pris pour ce client ?
    boolean existsByClient_IdclientsAndNomGroupeIgnoreCase(String clientId, String nomGroupe);

    // Un nom déjà pris par un AUTRE groupe que {id} pour ce client ?
    boolean existsByClient_IdclientsAndNomGroupeIgnoreCaseAndIdClientsGroupsNot(
            String clientId, String nomGroupe, String excludeId
    );
}
