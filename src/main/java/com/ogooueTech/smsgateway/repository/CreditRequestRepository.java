package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.model.CreditRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository chargé de gérer l'accès aux données
 * des demandes de crédit effectuées par les clients.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - enregistrer une demande de crédit ;
 * - rechercher une demande par son identifiant UUID ;
 * - récupérer les demandes ;
 * - modifier une demande ;
 * - supprimer une demande.
 *
 * L'entité gérée est {@link CreditRequest} et son identifiant
 * principal est de type {@link UUID}.
 */
public interface CreditRequestRepository
        extends JpaRepository<CreditRequest, UUID> {

    /**
     * Recherche une demande de crédit à partir :
     * - de l'identifiant du client ;
     * - de la clé d'idempotence fournie lors de la requête.
     *
     * Une clé d'idempotence permet d'éviter la création de plusieurs
     * demandes identiques lorsqu'un client renvoie la même requête,
     * par exemple après une erreur réseau ou un délai d'attente.
     *
     * La recherche est limitée au client concerné afin qu'une même clé
     * d'idempotence puisse éventuellement être utilisée par des clients
     * différents sans provoquer de conflit entre leurs demandes.
     *
     * @param clientId identifiant du client ayant créé la demande
     * @param idempotencyKey clé unique associée à la requête du client
     * @return demande trouvée dans un Optional, ou Optional.empty()
     *         si aucune demande ne correspond
     */
    Optional<CreditRequest> findByClient_IdclientsAndIdempotencyKey(
            String clientId,
            String idempotencyKey
    );

    /**
     * Recherche toutes les demandes de crédit appartenant
     * à un client précis.
     *
     * Les résultats sont paginés afin d'éviter de charger
     * toutes les demandes en mémoire en une seule fois.
     *
     * Le tri peut être défini dans l'objet Pageable.
     *
     * Exemple :
     * PageRequest.of(0, 20, Sort.by("createdAt").descending())
     *
     * @param clientId identifiant du client propriétaire des demandes
     * @param pageable paramètres de pagination et de tri
     * @return page contenant les demandes du client
     */
    Page<CreditRequest> findByClient_Idclients(
            String clientId,
            Pageable pageable
    );

    /**
     * Recherche les demandes de crédit d'un client
     * correspondant à un statut précis.
     *
     * Cette méthode peut par exemple être utilisée pour récupérer :
     * - les demandes en attente ;
     * - les demandes approuvées ;
     * - les demandes rejetées ;
     * - les demandes annulées.
     *
     * Les résultats sont paginés.
     *
     * @param clientId identifiant du client propriétaire des demandes
     * @param status statut des demandes recherchées
     * @param pageable paramètres de pagination et de tri
     * @return page contenant les demandes correspondant au client
     *         et au statut demandé
     */
    Page<CreditRequest> findByClient_IdclientsAndStatus(
            String clientId,
            CreditStatus status,
            Pageable pageable
    );

    /**
     * Recherche toutes les demandes de crédit correspondant
     * à un statut donné, tous clients confondus.
     *
     * Cette méthode est principalement destinée aux administrateurs
     * ou aux agents chargés de traiter les demandes de crédit.
     *
     * Les résultats sont paginés afin de limiter la quantité
     * de données chargée à chaque requête.
     *
     * @param status statut des demandes recherchées
     * @param pageable paramètres de pagination et de tri
     * @return page contenant les demandes correspondant au statut
     */
    Page<CreditRequest> findByStatus(
            CreditStatus status,
            Pageable pageable
    );
}