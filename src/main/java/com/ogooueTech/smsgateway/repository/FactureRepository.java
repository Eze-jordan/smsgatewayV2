package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
/**
 * Repository chargé de gérer l'accès aux données des factures.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - créer ou enregistrer une facture ;
 * - rechercher une facture par son identifiant ;
 * - récupérer toutes les factures ;
 * - modifier une facture ;
 * - supprimer une facture.
 *
 * L'entité gérée est {@link Facture} et son identifiant principal
 * est de type {@link String}.
 */
public interface FactureRepository extends JpaRepository<Facture, String> {
    /**
     * Recherche une facture correspondant exactement :
     * - à un client ;
     * - à une date de début ;
     * - à une date de fin.
     *
     * Cette méthode peut être utilisée avant la génération d'une facture
     * afin d'éviter de créer plusieurs factures pour la même période
     * et pour le même client.
     *
     * @param client client propriétaire de la facture
     * @param dateDebut date de début de la période facturée
     * @param dateFin date de fin de la période facturée
     * @return facture trouvée dans un Optional, ou Optional.empty()
     *         si aucune facture ne correspond
     */
    Optional<Facture> findByClientAndDateDebutAndDateFin(Client client, LocalDate dateDebut, LocalDate dateFin);
    /**
     * Recherche une facture par son identifiant en chargeant immédiatement :
     * - le client associé ;
     * - l'exercice comptable associé.
     *
     * Les clauses JOIN FETCH évitent des requêtes supplémentaires
     * lors de l'accès au client et à l'exercice après le chargement
     * de la facture.
     *
     * Cette méthode est utile pour afficher le détail complet
     * d'une facture.
     *
     * Attention : "f.id" doit correspondre exactement au nom
     * de l'attribut identifiant déclaré dans l'entité Facture.
     *
     * @param id identifiant de la facture recherchée
     * @return facture complète dans un Optional, ou Optional.empty()
     *         si aucune facture ne correspond
     */
    @Query("""
      select f from Facture f
      join fetch f.client c
      join fetch f.exercice e
      where f.id = :id
    """)
    Optional<Facture> findDetailById(String id);
    /**
     * Recherche toutes les factures appartenant à un client précis.
     *
     * La méthode utilise la relation "client" de l'entité Facture,
     * puis la propriété "idclients" de l'entité Client.
     *
     * @param clientId identifiant du client propriétaire des factures
     * @return liste des factures appartenant au client
     */
    List<Facture> findByClient_Idclients(String clientId);
    /**
     * Recherche les factures d'un client dont la période est entièrement
     * comprise entre les dates fournies.
     *
     * Conditions appliquées :
     * - la date de début de la facture doit être supérieure ou égale à start ;
     * - la date de fin de la facture doit être inférieure ou égale à end.
     *
     * Une facture qui commence avant start ou se termine après end
     * ne sera donc pas retournée.
     *
     * @param clientId identifiant du client propriétaire des factures
     * @param start date minimale de début de facture
     * @param end date maximale de fin de facture
     * @return liste des factures entièrement comprises dans la période
     */
    List<Facture> findByClient_IdclientsAndDateDebutGreaterThanEqualAndDateFinLessThanEqual(
            String clientId, LocalDate start, LocalDate end
    );


}