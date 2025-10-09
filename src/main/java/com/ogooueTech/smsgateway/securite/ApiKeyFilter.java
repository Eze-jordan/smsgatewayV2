package com.ogooueTech.smsgateway.securite;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ClientRepository clientRepository;

    public ApiKeyFilter(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ Normalise le chemin en minuscules
        String path = request.getServletPath().toLowerCase();

        // ✅ Ne protège que ces 3 endpoints SMS
        if (path.startsWith("/api/v1/sms/unides")
                || path.startsWith("/api/v1/sms/muldes")
                || path.startsWith("/api/v1/sms/muldesp")) {

            // Vérifie si un token JWT est déjà présent
            String authHeader = request.getHeader("Authorization");
            boolean hasJwt = (authHeader != null && authHeader.startsWith("Bearer "));

            // ✅ Si pas de JWT, on exige une clé API
            if (!hasJwt) {
                String apiKey = request.getHeader("X-API-Key");

                System.out.println("🔍 PATH : " + path);
                System.out.println("🔑 X-API-Key reçue : " + (apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) + "..." : "Aucune"));

                if (apiKey == null || apiKey.isBlank()) {
                    sendUnauthorized(response, "Clé API manquante");
                    return;
                }

                Optional<Client> clientOpt = clientRepository.findByCleApi(apiKey);

                if (clientOpt.isEmpty()) {
                    sendUnauthorized(response, "Clé API invalide");
                    return;
                }

                Client client = clientOpt.get();

                if (client.getStatutCompte() != StatutCompte.ACTIF) {
                    sendUnauthorized(response, "Compte client suspendu ou archivé");
                    return;
                }

                if (client.getCleApiExpiration() != null &&
                        client.getCleApiExpiration().isBefore(LocalDateTime.now())) {
                    sendUnauthorized(response, "Clé API expirée");
                    return;
                }

                // ✅ Clé valide → autoriser la requête
                request.setAttribute("client", client);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
