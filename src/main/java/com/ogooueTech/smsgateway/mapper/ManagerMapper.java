package com.ogooueTech.smsgateway.mapper;

import com.ogooueTech.smsgateway.dtos.ManagerDTO;
import com.ogooueTech.smsgateway.model.Manager;

public final class ManagerMapper {

    private ManagerMapper() {}

    public static ManagerDTO toDto(Manager e) {
        if (e == null) return null;
        ManagerDTO dto = new ManagerDTO();
        dto.setIdManager(e.getIdManager());
        dto.setNomManager(e.getNomManager());
        dto.setPrenomManager(e.getPrenomManager());
        dto.setEmail(e.getEmail());
        dto.setNumeroTelephoneManager(e.getNumeroTelephoneManager());
        dto.setRole(e.getRole());
        dto.setActif(e.isActif());
        // motDePasseManager: WRITE_ONLY
        return dto;
    }

    /** Pour création */
    public static Manager toEntity(ManagerDTO dto) {
        if (dto == null) return null;
        Manager e = new Manager();
        e.setNomManager(dto.getNomManager());
        e.setPrenomManager(dto.getPrenomManager());
        e.setEmail(dto.getEmail());
        e.setNumeroTelephoneManager(dto.getNumeroTelephoneManager());
        e.setRole(dto.getRole());
        e.setMotDePasseManager(dto.getMotDePasseManager()); // hash dans le service
        // actif: false par défaut dans l'entité
        return e;
    }

    /** Mise à jour partielle */
    public static void updateEntityFromDto(ManagerDTO dto, Manager e) {
        if (dto == null || e == null) return;

        if (dto.getNomManager() != null) e.setNomManager(dto.getNomManager());
        if (dto.getPrenomManager() != null) e.setPrenomManager(dto.getPrenomManager());
        if (dto.getEmail() != null) e.setEmail(dto.getEmail());
        if (dto.getNumeroTelephoneManager() != null) e.setNumeroTelephoneManager(dto.getNumeroTelephoneManager());
        if (dto.getRole() != null) e.setRole(dto.getRole());
        if (dto.getMotDePasseManager() != null) e.setMotDePasseManager(dto.getMotDePasseManager()); // hash service
        // actif : piloté par un workflow (activation) → pas exposé à update direct
    }
}
