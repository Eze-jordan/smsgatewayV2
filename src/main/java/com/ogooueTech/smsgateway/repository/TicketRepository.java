package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.StatutTicket;
import com.ogooueTech.smsgateway.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByClientId(String clientId);
    List<Ticket> findByStatut(StatutTicket statut);
}
