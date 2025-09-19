package com.ogooueTech.smsgateway.enums;

public enum StatutTicket {
    OUVERT,       // Ticket créé, en attente de prise en charge
    EN_COURS,     // Admin ou support est en train de le traiter
    FERME,        // Ticket fermé
    URGENT        // Marqué comme urgent
}
