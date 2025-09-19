package com.ogooueTech.smsgateway.dtos;

import java.time.LocalDate;
import java.util.List;


public record MuldespRequest(
        String clientId,
        String emetteur,
        List<String> numeros,
        String corps,
        LocalDate dateDebut,      // juste la date
        Integer nbParJour,
        Integer intervalleMinutes,
        LocalDate dateFin         // juste la date
) {}


