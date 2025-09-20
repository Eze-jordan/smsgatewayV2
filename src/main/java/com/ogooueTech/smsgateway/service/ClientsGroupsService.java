package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.ClientsGroups;

import java.util.List;

public interface ClientsGroupsService {
    ClientsGroups create(String clientId, String nom, String description);
    List<ClientsGroups> listByClient(String clientId);
    ClientsGroups get(String id);
    ClientsGroups patch(String id, String nom, String description);
    void delete(String id);
    List<ClientsGroups> listAll();
    List<ClientsGroups> searchByClient(String clientId, String keyword);


}
