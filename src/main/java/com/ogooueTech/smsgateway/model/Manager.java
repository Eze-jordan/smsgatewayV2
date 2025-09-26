package com.ogooueTech.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
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
    @Column(unique = true, nullable = false)
    private String email;
    @Column(unique = true, nullable = false)
    private String numeroTelephoneManager;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte; // ACTIF ou SUSPENDU




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
    public boolean isAccountNonExpired() {
        return true; // tu ne gères pas l’expiration de compte
    }

    @Override
    public boolean isAccountNonLocked() {
        // Si suspendu, compte verrouillé
        return this.statutCompte != StatutCompte.SUSPENDU;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // tu ne gères pas l’expiration de credentials
    }

    @Override
    public boolean isEnabled() {
        // Actif uniquement si statut = ACTIF
        return this.statutCompte == StatutCompte.ACTIF;
    }

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
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
