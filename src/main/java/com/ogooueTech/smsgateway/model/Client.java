package com.ogooueTech.smsgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import jakarta.persistence.*;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "clients")
public class Client implements UserDetails {

    @Id
    @Column(name = "id_client", length = 6, nullable = false, unique = true)
    private String idclients;
// identifiant technique interne
    private String raisonSociale;
    private String secteurActivite;
    private String ville;
    @Column(length = 255)
    private String adresse;
    private String telephone;
    @Column(unique = true, nullable = false)
    private String email;
    private String nif;
    private String rccm;
    /**
     * Mémorise le dernier mouvement de solde :
     * - PREPAYE : dernier crédit (nb de SMS ajoutés)
     * - POSTPAYE: dernière conso facturée (nb de SMS)
     */
    @Column(name = "last_solde_net")
    private Integer lastSoldeNet;

    // alias expéditeur (11 caractères max selon les pratiques SMS)
    @Column(length = 11)
    private String emetteur;
    private BigDecimal coutSmsTtc;
    @Enumerated(EnumType.STRING)
    private TypeCompte typeCompte; // PREPAYE ou POSTPAYE

    // PREPAYE => nb sms restants ; POSTPAYE => consommation du mois
    private Integer soldeNet;
    @JsonIgnore
    @ToString.Exclude
    private String motDePasse; // à hasher (BCrypt) côté Manager/espace

    @Column(unique = true, nullable = false)
    private String cleApi; // utilisée pour l’API (X-API-Key)
    // Client.java
    @Enumerated(EnumType.STRING)        // persiste l’ENUM en clair
    private Role role;


    @Enumerated(EnumType.STRING)
    private StatutCompte statutCompte; // ACTIF ou SUSPENDU


    public String getIdclients() {
        return idclients;
    }

    public void setIdclients(String idclients) {
        this.idclients = idclients;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public String getSecteurActivite() {
        return secteurActivite;
    }

    public void setSecteurActivite(String secteurActivite) {
        this.secteurActivite = secteurActivite;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public String getRccm() {
        return rccm;
    }

    public void setRccm(String rccm) {
        this.rccm = rccm;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public BigDecimal getCoutSmsTtc() {
        return coutSmsTtc;
    }

    public void setCoutSmsTtc(BigDecimal coutSmsTtc) {
        this.coutSmsTtc = coutSmsTtc;
    }

    public TypeCompte getTypeCompte() {
        return typeCompte;
    }

    public void setTypeCompte(TypeCompte typeCompte) {
        this.typeCompte = typeCompte;
    }

    public Integer getSoldeNet() {
        return soldeNet;
    }

    public void setSoldeNet(Integer soldeNet) {
        this.soldeNet = soldeNet;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getCleApi() {
        return cleApi;
    }

    public void setCleApi(String cleApi) {
        this.cleApi = cleApi;
    }

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public Integer getLastSoldeNet() {
        return lastSoldeNet;
    }

    public void setLastSoldeNet(Integer lastSoldeNet) {
        this.lastSoldeNet = lastSoldeNet;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + this.role));
    }


    @Override
        public String getPassword() {
        return this.motDePasse;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

}
