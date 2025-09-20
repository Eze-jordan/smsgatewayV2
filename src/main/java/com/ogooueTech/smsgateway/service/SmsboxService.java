package com.ogooueTech.smsgateway.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class SmsboxService {

    private static final String SMSBOX_URL = "https://api.smsbox.fr/1.1/api.php";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envoi d’un SMS via SMSBOX
     * @param apiKey clé API SMSBOX
     * @param dest numéro destinataire (ex: 33600123456 ou plusieurs séparés par virgules)
     * @param msg message à envoyer
     * @param mode Standard ou Expert
     * @param strategy stratégie (1 à 4 selon type message)
     * @return réponse brute de SMSBOX (OK..., ERROR...)
     */
    public String sendSms(String apiKey, String dest, String msg, String mode, int strategy) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(SMSBOX_URL)
                    .queryParam("apikey", apiKey)
                    .queryParam("dest", dest)
                    .queryParam("msg", msg)
                    .queryParam("mode", mode)
                    .queryParam("strategy", strategy)
                    .encode(StandardCharsets.UTF_8)
                    .build()
                    .toUri();

            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erreur envoi SMSBOX: " + e.getMessage(), e);
        }
    }
}
