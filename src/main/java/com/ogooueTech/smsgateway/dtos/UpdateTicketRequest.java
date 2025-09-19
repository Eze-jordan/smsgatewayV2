package com.ogooueTech.smsgateway.dtos;

import com.ogooueTech.smsgateway.enums.StatutTicket;

public record UpdateTicketRequest(
        StatutTicket statut,
        String reponseAdmin
) {}
