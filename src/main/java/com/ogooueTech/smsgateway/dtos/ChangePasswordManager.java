package com.ogooueTech.smsgateway.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChangePasswordManager {
    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String oldPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères")
    private String newPassword;

    public @NotBlank(message = "L'ancien mot de passe est obligatoire") String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(@NotBlank(message = "L'ancien mot de passe est obligatoire") String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public @NotBlank(message = "Le nouveau mot de passe est obligatoire") @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères") String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(@NotBlank(message = "Le nouveau mot de passe est obligatoire") @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères") String newPassword) {
        this.newPassword = newPassword;
    }
}
