package com.ogooueTech.smsgateway.securite;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
public class CustomUserDetails implements UserDetails {
    private final String id;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String nom;
    private final String role;
    private final Boolean abonneExpire;
    private final String statutCompte; // ✅ ajout


    public CustomUserDetails(String id, String email, String password,
                             Collection<? extends GrantedAuthority> authorities,
                             String nom, String role, Boolean abonneExpire, String statutCompte) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.nom = nom;
        this.role = role;
        this.abonneExpire = abonneExpire;
        this.statutCompte = statutCompte;
    }
    public String getStatutCompte() { return statutCompte; }

    public String getId() { return id; }

    public String getNom() {
        return nom;
    }

    public String getRole() {
        return role;
    }

    public Boolean getAbonneExpire() {
        return abonneExpire;}

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }


    @Override
    public boolean isCredentialsNonExpired() { return true; }


    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDU".equalsIgnoreCase(statutCompte);
    }

    @Override
    public boolean isEnabled() {
        return "ACTIF".equalsIgnoreCase(statutCompte);
    }
}