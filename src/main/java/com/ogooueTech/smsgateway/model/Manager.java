package com.ogooueTech.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ogooueTech.smsgateway.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "Manager_admin")
public class Manager  implements UserDetails {
    @Id
    @Column(name = "id_manager", length = 6, nullable = false, unique = true)
    private String idManager;// identifiant technique interne
    private String nomManager;
    private String prenomManager;
    @JsonIgnore
    private String motDePasseManager;
    private String email;
    private String numeroTelephoneManager;
    @Enumerated(EnumType.STRING)
    private Role role;



    public String getIdManager() {
        return idManager;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getMotDePasseManager() {
        return motDePasseManager;
    }

    public void setMotDePasseManager(String motDePasseManager) {
        this.motDePasseManager = motDePasseManager;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumeroTelephoneManager() {
        return numeroTelephoneManager;
    }

    public void setNumeroTelephoneManager(String numeroTelephoneManager) {
        this.numeroTelephoneManager = numeroTelephoneManager;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role));
    }



    @Override
    public String getPassword() {
        return this.motDePasseManager;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

}
