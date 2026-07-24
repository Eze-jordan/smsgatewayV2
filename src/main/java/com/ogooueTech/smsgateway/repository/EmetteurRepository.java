package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Emetteur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository chargé de gérer l'accès aux données des émetteurs SMS.
 *
 * Un émetteur représente généralement le nom ou l'identifiant
 * affiché comme expéditeur d'un SMS.
 *
 * JpaRepository fournit automatiquement les principales opérations CRUD :
 * - enregistrer un émetteur ;
 * - rechercher un émetteur par son identifiant ;
 * - récupérer tous les émetteurs ;
 * - modifier un émetteur ;
 * - supprimer un émetteur.
 *
 * L'entité gérée est {@link Emetteur} et son identifiant principal
 * est de type {@link String}.
 */
public interface EmetteurRepository extends JpaRepository<Emetteur, String> {

    /**
     * Recherche tous les émetteurs appartenant à un client précis.
     *
     * La méthode utilise la relation "client" présente dans l'entité
     * Emetteur, puis la propriété "idclients" de l'entité Client.
     *
     * Équivalent logique :
     * emetteur.client.idclients = clientId
     *
     * @param clientId identifiant du client propriétaire des émetteurs
     * @return liste des émetteurs appartenant au client
     */
    List<Emetteur> findByClient_Idclients(
            String clientId
    );

    /**
     * Vérifie si un émetteur portant le même nom existe déjà
     * pour un client donné.
     *
     * La comparaison ne tient pas compte des majuscules
     * et minuscules.
     *
     * Exemple :
     * "OGOOUETECH" et "ogoouetech" seront considérés
     * comme étant le même nom.
     *
     * Cette méthode peut être utilisée avant la création
     * d'un émetteur afin d'éviter les doublons pour un même client.
     *
     * @param clientId identifiant du client propriétaire de l'émetteur
     * @param nom nom de l'émetteur à vérifier
     * @return true si ce nom existe déjà pour le client, sinon false
     */
    boolean existsByClient_IdclientsAndNomIgnoreCase(
            String clientId,
            String nom
    );
}