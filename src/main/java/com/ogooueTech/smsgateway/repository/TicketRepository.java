package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutTicket;
import com.ogooueTech.smsgateway.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository chargé de gérer l'accès aux données des tickets.
 *
 * Un ticket représente généralement une demande d'assistance,
 * un incident ou une réclamation créée par un client.
 *
 * JpaRepository fournit automatiquement les opérations CRUD principales :
 * - créer ou enregistrer un ticket ;
 * - rechercher un ticket par son identifiant UUID ;
 * - récupérer tous les tickets ;
 * - modifier un ticket ;
 * - supprimer un ticket.
 *
 * L'entité gérée est {@link Ticket} et son identifiant principal
 * est de type {@link UUID}.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /**
     * Recherche tous les tickets appartenant à un client précis.
     *
     * Cette méthode suppose que l'entité Ticket contient directement
     * un attribut nommé "clientId".
     *
     * Si Ticket contient plutôt une relation appelée "client",
     * la méthode devra être nommée :
     * findByClient_Idclients(String clientId).
     *
     * @param clientId identifiant du client propriétaire des tickets
     * @return liste des tickets appartenant au client
     */
    List<Ticket> findByClientId(
            String clientId
    );

    /**
     * Recherche tous les tickets correspondant à un statut précis.
     *
     * Cette méthode peut par exemple permettre de récupérer :
     * - les tickets ouverts ;
     * - les tickets en cours de traitement ;
     * - les tickets résolus ;
     * - les tickets fermés.
     *
     * @param statut statut des tickets recherchés
     * @return liste des tickets correspondant au statut
     */
    List<Ticket> findByStatut(
            StatutTicket statut
    );
}