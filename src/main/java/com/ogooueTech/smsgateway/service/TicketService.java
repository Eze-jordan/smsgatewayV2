package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.enums.StatutTicket;
import com.ogooueTech.smsgateway.model.Ticket;
import com.ogooueTech.smsgateway.repository.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /** Création d’un ticket par un client */
    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    /** Liste tous les tickets */
    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    /** Liste des tickets d’un client */
    public List<Ticket> findByClient(String clientId) {
        return ticketRepository.findByClientId(clientId);
    }

    /** Met à jour le statut ou la réponse (par admin) */
    public Ticket updateStatut(UUID id, StatutTicket statut, String reponseAdmin) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket introuvable: " + id));

        ticket.setStatut(statut);
        if (reponseAdmin != null) {
            ticket.setReponseAdmin(reponseAdmin);
        }
        return ticketRepository.save(ticket);
    }
}
