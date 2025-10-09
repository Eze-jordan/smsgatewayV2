package com.ogooueTech.smsgateway.securite;

import com.ogooueTech.smsgateway.enums.StatutCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ClientRepository clientRepository;
    // ✅ Injection par constructeur explicite
    public ApiKeyFilter(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ✅ On ne protège que les endpoints SMS
        if (path.startsWith("/api/V1/sms")) {
            String apiKey = request.getHeader("X-API-Key");

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

            // ✅ Clé valide → on autorise la requête
            request.setAttribute("client", client);
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
