package com.ogooueTech.smsgateway.mapper;

import com.ogooueTech.smsgateway.dtos.ClientDTO;
import com.ogooueTech.smsgateway.model.Client;

public final class ClientMapper {

    private ClientMapper() {}

    public static ClientDTO toDto(Client e) {
        if (e == null) return null;
        ClientDTO dto = new ClientDTO();
        dto.setIdclients(e.getIdclients());
        dto.setRaisonSociale(e.getRaisonSociale());
        dto.setSecteurActivite(e.getSecteurActivite());
        dto.setVille(e.getVille());
        dto.setAdresse(e.getAdresse());
        dto.setTelephone(e.getTelephone());
        dto.setEmail(e.getEmail());
        dto.setNif(e.getNif());
        dto.setRccm(e.getRccm());
        dto.setEmetteur(e.getEmetteur());
        dto.setCoutSmsTtc(e.getCoutSmsTtc());
        dto.setTypeCompte(e.getTypeCompte());
        dto.setRole(e.getRole());
        dto.setSoldeNet(e.getSoldeNet());
        dto.setStatutCompte(e.getStatutCompte());
        dto.setCleApi(e.getCleApi());
        // motDePasse: WRITE_ONLY → on ne le met pas
        return dto;
    }

    /** Pour création (les champs READ_ONLY sont ignorés) */
    public static Client toEntity(ClientDTO dto) {
        if (dto == null) return null;
        Client e = new Client();
        e.setRaisonSociale(dto.getRaisonSociale());
        e.setSecteurActivite(dto.getSecteurActivite());
        e.setVille(dto.getVille());
        e.setAdresse(dto.getAdresse());
        e.setTelephone(dto.getTelephone());
        e.setEmail(dto.getEmail());
        e.setNif(dto.getNif());
        e.setRccm(dto.getRccm());
        e.setEmetteur(dto.getEmetteur());
        e.setCoutSmsTtc(dto.getCoutSmsTtc());
        e.setTypeCompte(dto.getTypeCompte());
        e.setRole(dto.getRole());
        // motDePasse : à hasher dans le service si présent
        return e;
    }

    /** Mise à jour partielle : copie uniquement les champs non-nuls du DTO */
    public static void updateEntityFromDto(ClientDTO dto, Client e) {
        if (dto == null || e == null) return;

        if (dto.getRaisonSociale() != null) e.setRaisonSociale(dto.getRaisonSociale());
        if (dto.getSecteurActivite() != null) e.setSecteurActivite(dto.getSecteurActivite());
        if (dto.getVille() != null) e.setVille(dto.getVille());
        if (dto.getAdresse() != null) e.setAdresse(dto.getAdresse());
        if (dto.getTelephone() != null) e.setTelephone(dto.getTelephone());
        if (dto.getEmail() != null) e.setEmail(dto.getEmail());
        if (dto.getNif() != null) e.setNif(dto.getNif());
        if (dto.getRccm() != null) e.setRccm(dto.getRccm());
        if (dto.getEmetteur() != null) e.setEmetteur(dto.getEmetteur());
        if (dto.getCoutSmsTtc() != null) e.setCoutSmsTtc(dto.getCoutSmsTtc());
        if (dto.getTypeCompte() != null) e.setTypeCompte(dto.getTypeCompte());
        if (dto.getRole() != null) e.setRole(dto.getRole());

        // Champs sensibles / contrôlés côté service :
        if (dto.getMotDePasse() != null) e.setMotDePasse(dto.getMotDePasse()); // hash dans le service
        // soldeNet, statutCompte, cleApi : gérés côté métier/manager → on ne touche pas ici
    }
}
