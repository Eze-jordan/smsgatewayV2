package com.ogooueTech.smsgateway.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ogooueTech.smsgateway.enums.Role;
import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientDTO {

    // Lecture seule (généré par la DB)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String idclients;

    // Données d'identité
    @Size(max = 255)
    private String raisonSociale;
    private String secteurActivite;
    private String ville;
    private String adresse;
    private String telephone;

    @Email
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    private String nif;
    private String rccm;
    // Alias expéditeur (11 chars max)
    @Size(max = 11)
    private String emetteur;
    private BigDecimal coutSmsTtc;
    private TypeCompte typeCompte;
    private Role role;
    // Champs métier/état
    private Integer soldeNet;
    private StatutCompte statutCompte;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cleApi;
    // Mot de passe : on l’accepte en entrée mais on ne le renvoie jamais
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String motDePasse;

    public String getIdclients() {
        return idclients;
    }

    public void setIdclients(String idclients) {
        this.idclients = idclients;
    }

    public @Size(max = 255) String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(@Size(max = 255) String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public String getSecteurActivite() {
        return secteurActivite;
    }

    public void setSecteurActivite(String secteurActivite) {
        this.secteurActivite = secteurActivite;
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

    public @Email @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String getEmail() {
        return email;
    }

    public void setEmail(@Email @NotBlank(message = "L'email est obligatoire") @Email(message = "Email invalide") String email) {
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

    public @Size(max = 11) String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(@Size(max = 11) String emetteur) {
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Integer getSoldeNet() {
        return soldeNet;
    }

    public void setSoldeNet(Integer soldeNet) {
        this.soldeNet = soldeNet;
    }

    public StatutCompte getStatutCompte() {
        return statutCompte;
    }

    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = statutCompte;
    }

    public String getCleApi() {
        return cleApi;
    }

    public void setCleApi(String cleApi) {
        this.cleApi = cleApi;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}