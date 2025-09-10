package com.ogooueTech.smsgateway.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogooueTech.smsgateway.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManagerDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String idManager;

    private String nomManager;
    private String prenomManager;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String numeroTelephoneManager;
    private Role role;

    private Boolean actif;

    // Accepté en entrée, jamais renvoyé
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
    )
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

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public @NotBlank(message = "Le mot de passe est obligatoire") @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères") @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
    ) String getMotDePasseManager() {
        return motDePasseManager;
    }

    public void setMotDePasseManager(@NotBlank(message = "Le mot de passe est obligatoire") @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères") @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial"
    ) String motDePasseManager) {
        this.motDePasseManager = motDePasseManager;
    }
}