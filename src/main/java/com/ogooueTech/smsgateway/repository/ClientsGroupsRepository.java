package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.ClientsGroups;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository chargé de gérer l'accès aux données
 * des groupes de contacts appartenant aux clients.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - créer ou enregistrer un groupe ;
 * - rechercher un groupe par son identifiant ;
 * - récupérer tous les groupes ;
 * - modifier un groupe ;
 * - supprimer un groupe.
 *
 * L'entité gérée est {@link ClientsGroups} et son identifiant
 * principal est de type {@link String}.
 */
public interface ClientsGroupsRepository
        extends JpaRepository<ClientsGroups, String> {

    /**
     * Recherche tous les groupes appartenant à un client précis.
     *
     * La méthode utilise la relation "client" présente dans l'entité
     * ClientsGroups, puis la propriété "idclients" de l'entité Client.
     *
     * Équivalent logique :
     * groupe.client.idclients = clientId
     *
     * @param clientId identifiant du client propriétaire des groupes
     * @return liste des groupes appartenant au client
     */
    List<ClientsGroups> findAllByClient_Idclients(
            String clientId
    );

    /**
     * Vérifie si un groupe portant le même nom existe déjà
     * pour un client donné.
     *
     * La comparaison du nom du groupe ne tient pas compte
     * des majuscules et minuscules.
     *
     * Exemple :
     * "Clients VIP" et "clients vip" seront considérés
     * comme étant le même nom.
     *
     * Cette méthode est principalement utilisée lors
     * de la création d'un nouveau groupe afin d'éviter
     * les doublons pour un même client.
     *
     * @param clientId identifiant du client propriétaire du groupe
     * @param nomGroupe nom du groupe à vérifier
     * @return true si un groupe portant ce nom existe déjà,
     *         sinon false
     */
    boolean existsByClient_IdclientsAndNomGroupeIgnoreCase(
            String clientId,
            String nomGroupe
    );

    /**
     * Vérifie si un autre groupe possède déjà le même nom
     * pour un client donné.
     *
     * Le groupe dont l'identifiant correspond à excludeId
     * est exclu de la recherche.
     *
     * Cette méthode est utile lors de la modification d'un groupe.
     * Elle permet de vérifier que le nouveau nom n'est pas déjà utilisé
     * par un autre groupe du même client.
     *
     * Exemple :
     * - le groupe actuellement modifié possède l'identifiant "GRP-001" ;
     * - excludeId vaut donc "GRP-001" ;
     * - la recherche vérifie tous les autres groupes du client.
     *
     * La comparaison du nom ne tient pas compte
     * des majuscules et minuscules.
     *
     * @param clientId identifiant du client propriétaire du groupe
     * @param nomGroupe nom du groupe à vérifier
     * @param excludeId identifiant du groupe à exclure de la recherche
     * @return true si un autre groupe porte déjà ce nom,
     *         sinon false
     */
    boolean existsByClient_IdclientsAndNomGroupeIgnoreCaseAndIdClientsGroupsNot(
            String clientId,
            String nomGroupe,
            String excludeId
    );

    /**
     * Recherche les groupes d'un client dont le nom contient
     * un mot-clé donné.
     *
     * La recherche ne tient pas compte des majuscules
     * et minuscules.
     *
     * Exemple :
     * le mot-clé "vip" peut retrouver les groupes :
     * - "Clients VIP" ;
     * - "VIP Entreprises" ;
     * - "Liste vip".
     *
     * @param clientId identifiant du client propriétaire des groupes
     * @param keyword texte recherché dans le nom du groupe
     * @return liste des groupes correspondant au mot-clé
     */
    List<ClientsGroups>
    findByClient_IdclientsAndNomGroupeContainingIgnoreCase(
            String clientId,
            String keyword
    );
}