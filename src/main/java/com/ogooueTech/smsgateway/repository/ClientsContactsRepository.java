package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.ClientsContacts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientsContactsRepository extends JpaRepository<ClientsContacts, String> {

    List<ClientsContacts> findAllByClientsGroup_IdClientsGroupsOrderByCreatedAtDesc(String groupId);

    // ✅ désormais on vérifie par client dénormalisé
    boolean existsByClientIdAndContactNumberIgnoreCase(String clientId, String contactNumber);
}
