package com.ogooueTech.smsgateway.dtos;

import jakarta.validation.constraints.NotBlank;


public class ChangePasswordRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String oldPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    private String newPassword;

    public @NotBlank(message = "L'ancien mot de passe est obligatoire") String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(@NotBlank(message = "L'ancien mot de passe est obligatoire") String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public @NotBlank(message = "Le nouveau mot de passe est obligatoire") String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(@NotBlank(message = "Le nouveau mot de passe est obligatoire") String newPassword) {
        this.newPassword = newPassword;
    }
}
