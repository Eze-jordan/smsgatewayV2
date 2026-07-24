package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.ClientsContacts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository chargé de gérer l'accès aux données des contacts clients.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - enregistrer un contact ;
 * - rechercher un contact par son identifiant ;
 * - récupérer tous les contacts ;
 * - modifier un contact ;
 * - supprimer un contact.
 *
 * L'entité gérée est {@link ClientsContacts} et son identifiant
 * principal est de type {@link String}.
 */
public interface ClientsContactsRepository
        extends JpaRepository<ClientsContacts, String> {

    /**
     * Recherche tous les contacts appartenant à un groupe de contacts précis.
     *
     * La recherche utilise la relation "clientsGroup" de l'entité
     * ClientsContacts, puis l'identifiant "idClientsGroups"
     * présent dans l'entité du groupe.
     *
     * Les contacts sont triés par date de création décroissante :
     * les contacts les plus récents apparaissent en premier.
     *
     * @param groupId identifiant du groupe de contacts
     * @return liste des contacts appartenant au groupe demandé
     */
    List<ClientsContacts>
    findAllByClientsGroup_IdClientsGroupsOrderByCreatedAtDesc(
            String groupId
    );

    /**
     * Vérifie si un numéro de téléphone est déjà enregistré
     * pour un client précis.
     *
     * La vérification utilise le champ clientId dénormalisé,
     * directement stocké dans l'entité ClientsContacts.
     *
     * La comparaison du numéro ne tient pas compte
     * des majuscules et minuscules.
     *
     * Cette méthode peut être utilisée avant l'ajout d'un contact
     * afin d'éviter les doublons dans le carnet d'adresses d'un client.
     *
     * @param clientId identifiant du client propriétaire du contact
     * @param contactNumber numéro du contact à vérifier
     * @return true si le numéro existe déjà pour ce client, sinon false
     */
    boolean existsByClientIdAndContactNumberIgnoreCase(
            String clientId,
            String contactNumber
    );

    /**
     * Recherche les contacts dont le numéro contient le premier mot-clé
     * ou dont le nom contient le second mot-clé.
     *
     * La recherche ne tient pas compte des majuscules et minuscules.
     *
     * Attention : cette méthode recherche dans les contacts de tous
     * les clients, car aucun identifiant client n'est fourni.
     *
     * @param number mot-clé recherché dans le numéro du contact
     * @param name mot-clé recherché dans le nom du contact
     * @return liste des contacts correspondant au numéro ou au nom
     */
    List<ClientsContacts>
    findByContactNumberContainingIgnoreCaseOrContactNameContainingIgnoreCase(
            String number,
            String name
    );

    /**
     * Recherche tous les contacts appartenant à un client précis.
     *
     * Les contacts sont triés par date de création décroissante :
     * les contacts les plus récents apparaissent en premier.
     *
     * @param clientId identifiant du client propriétaire des contacts
     * @return liste des contacts du client triée du plus récent au plus ancien
     */
    List<ClientsContacts> findAllByClientIdOrderByCreatedAtDesc(
            String clientId
    );

    /**
     * Recherche les contacts d'un client dont :
     * - le numéro contient un mot-clé ;
     * - ou le nom contient un mot-clé.
     *
     * Le clientId doit être transmis deux fois, car Spring Data JPA
     * interprète la méthode comme l'expression suivante :
     *
     * (clientId = clientId1 ET numéro contient numberKeyword)
     * OU
     * (clientId = clientId2 ET nom contient nameKeyword)
     *
     * Pour limiter correctement la recherche au même client,
     * clientId1 et clientId2 doivent donc contenir la même valeur.
     *
     * La recherche ne tient pas compte des majuscules et minuscules.
     *
     * @param clientId1 identifiant du client utilisé pour la recherche par numéro
     * @param numberKeyword mot-clé recherché dans le numéro du contact
     * @param clientId2 identifiant du client utilisé pour la recherche par nom
     * @param nameKeyword mot-clé recherché dans le nom du contact
     * @return liste des contacts correspondant au numéro ou au nom
     */
    List<ClientsContacts>
    findByClientIdAndContactNumberContainingIgnoreCaseOrClientIdAndContactNameContainingIgnoreCase(
            String clientId1,
            String numberKeyword,
            String clientId2,
            String nameKeyword
    );
}