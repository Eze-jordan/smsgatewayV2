package com.ogooueTech.smsgateway.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagerDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String idManager;

    private String nomManager;
    private String prenomManager;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String numeroTelephoneManager;
    private Role role;
    private StatutCompte statutCompte;

    // Optionnel maintenant (généré côté backend)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String motDePasseManager;

    public String getIdManager() {
        return idManager;
    }

    public void setIdManager(String idManager) {
        this.idManager = idManager;
    }

    public String getNomManager() {
        return nomManager;
    }

    public void setNomManager(String nomManager) {
        this.nomManager = nomManager;
    }

    public String getPrenomManager() {
        return prenomManager;
    }

    public void setPrenomManager(String prenomManager) {
        this.prenomManager = prenomManager;
    }

    public @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email) {
        this.email = email;
    }

    public String getNumeroTelephoneManager() {
        return numeroTelephoneManager;
    }

    public void setNumeroTelephoneManager(String numeroTelephoneManager) {
        this.numeroTelephoneManager = numeroTelephoneManager;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getMotDePasseManager() {
        return motDePasseManager;
    }

    public void setMotDePasseManager(String motDePasseManager) {
        this.motDePasseManager = motDePasseManager;
    }

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
    }
}
