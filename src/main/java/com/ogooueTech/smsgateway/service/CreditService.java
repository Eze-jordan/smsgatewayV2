package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.CreditRequestDto;
import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.CreditRequest;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.CreditRequestRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class CreditService {

    private final CreditRequestRepository creditRepo;
    private final ClientRepository clientRepo;

    public CreditService(CreditRequestRepository creditRepo, ClientRepository clientRepo) {
        this.creditRepo = creditRepo;
        this.clientRepo = clientRepo;
    }

    public CreditRequestDto create(String clientId, int quantity, String idempotencyKey) {
        if (clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("clientId requis");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity doit être > 0");
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new IllegalArgumentException("idempotencyKey requis");

        // Idempotence
        var existing = creditRepo.findByIdempotencyKey(idempotencyKey.trim());
        if (existing.isPresent()) {
            CreditRequest e = existing.get();
            var prixSms = e.getClient().getCoutSmsTtc();
            if (prixSms == null) throw new IllegalStateException("Coût SMS (TTC) non défini pour le client");
            var estimated = prixSms.multiply(java.math.BigDecimal.valueOf(e.getQuantity()))
                    .setScale(0, java.math.RoundingMode.HALF_UP);
            return CreditRequestDto.from(e, prixSms, estimated);
        }

        // Verrouille le client
        Client client = clientRepo.lockById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable: " + clientId));

        // Seuls PREPAYE
        if (client.getTypeCompte() != TypeCompte.PREPAYE) {
            throw new IllegalStateException("Seuls les comptes PRÉPAYÉS peuvent demander un crédit. Type actuel: " + client.getTypeCompte());
        }

        // Prix unitaire
        java.math.BigDecimal prixSms = client.getCoutSmsTtc();
        if (prixSms == null)
            throw new IllegalStateException("Coût SMS (TTC) non défini pour le client");

        // Maker = email client
        String makerEmail = client.getEmail();
        if (makerEmail == null || makerEmail.isBlank()) {
            throw new IllegalStateException("Email du client manquant: impossible de définir le maker");
        }

        CreditRequest req = new CreditRequest();
        req.setClient(client);
        req.setQuantity(quantity);
        req.setMakerEmail(makerEmail);
        req.setIdempotencyKey(idempotencyKey.trim());
        req.setStatus(CreditStatus.PENDING);

        // Montant estimé TTC
        java.math.BigDecimal estimated = prixSms.multiply(java.math.BigDecimal.valueOf(quantity))
                .setScale(0, java.math.RoundingMode.HALF_UP);

        CreditRequest saved = creditRepo.save(req);
        return CreditRequestDto.from(saved, prixSms, estimated);
    }


    /** Checker: approuve => crédite le compte prépayé et clôture la demande */
    public CreditRequestDto approve(UUID requestId, String checkerEmail) {
        if (checkerEmail == null || checkerEmail.isBlank())
            throw new IllegalArgumentException("checkerEmail requis");

        CreditRequest req = creditRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getStatus() != CreditStatus.PENDING) {
            throw new IllegalStateException("La demande n'est plus en attente");
        }
        if (req.getMakerEmail() != null && req.getMakerEmail().equalsIgnoreCase(checkerEmail)) {
            throw new IllegalArgumentException("Le Maker ne peut pas valider sa propre demande");
        }

        // Sécurité: quantité strictement positive
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantité de crédit invalide");
        }

        // Verrouille le client avant MAJ du solde
        Client client = clientRepo.lockById(req.getClient().getIdclients())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));

        // Vérifie PREPAYE via l'enum (évite la comparaison sur String)
        if (client.getTypeCompte() != TypeCompte.PREPAYE) {
            throw new IllegalStateException("Crédit autorisé uniquement pour les comptes PREPAYE");
        }

        int oldSolde = client.getSoldeNet() == null ? 0 : client.getSoldeNet();
        int credited = req.getQuantity();

        // ✅ PREPAYE : mémoriser la dernière quantité créditée
        client.setSoldeNet(oldSolde + credited);
        client.setLastSoldeNet(credited);
        clientRepo.save(client);

        req.setStatus(CreditStatus.APPROVED);
        req.setCheckerEmail(checkerEmail);
        req.setValidatedAt(LocalDateTime.now());

        return CreditRequestDto.from(creditRepo.save(req));
    }


    /** Checker: rejette une demande (sans impact solde) */
    public CreditRequestDto reject(UUID requestId, String checkerEmail, String reason) {
        if (checkerEmail == null || checkerEmail.isBlank()) throw new IllegalArgumentException("checkerEmail requis");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason requis");

        CreditRequest req = creditRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getStatus() != CreditStatus.PENDING) {
            throw new IllegalStateException("La demande n'est plus en attente");
        }

        req.setStatus(CreditStatus.REJECTED);
        req.setCheckerEmail(checkerEmail);
        req.setRejectReason(reason.trim());
        req.setValidatedAt(LocalDateTime.now());

        return CreditRequestDto.from(creditRepo.save(req));
    }

    /** Liste paginée */
    @Transactional(readOnly = true)
    public Page<CreditRequestDto> list(String clientId, CreditStatus status, Pageable pageable) {
        Page<CreditRequest> page;
        if (clientId != null && !clientId.isBlank() && status != null) {
            page = creditRepo.findByClient_IdclientsAndStatus(clientId, status, pageable);
        } else if (clientId != null && !clientId.isBlank()) {
            page = creditRepo.findByClient_Idclients(clientId, pageable);
        } else if (status != null) {
            page = creditRepo.findByStatus(status, pageable);
        } else {
            page = creditRepo.findAll(pageable);
        }
        return page.map(CreditRequestDto::from);
    }

    /* -------- Helpers d’adaptation aux noms de champs de Client -------- */

    // ⚠️ Adapte ces méthodes en fonction de ta classe Client (noms camelCase).
    private Object getTypeCompte(Client c) {
        try {
            var m = c.getClass().getMethod("getTypeCompte");
            return m.invoke(c);
        } catch (Exception ignore) {
            try {
                var m = c.getClass().getMethod("getType_compte");
                return m.invoke(c);
            } catch (Exception e) {
                throw new IllegalStateException("Client: getter typeCompte/type_compte introuvable");
            }
        }
    }

    private int getSoldeNet(Client c) {
        try {
            var m = c.getClass().getMethod("getSoldeNet");
            Object v = m.invoke(c);
            return v == null ? 0 : ((Number) v).intValue();
        } catch (Exception ignore) {
            try {
                var m = c.getClass().getMethod("getSolde_net");
                Object v = m.invoke(c);
                return v == null ? 0 : ((Number) v).intValue();
            } catch (Exception e) {
                throw new IllegalStateException("Client: getter soldeNet/solde_net introuvable");
            }
        }
    }

    private void setSoldeNet(Client c, int value) {
        try {
            var m = c.getClass().getMethod("setSoldeNet", Integer.class);
            m.invoke(c, value);
        } catch (Exception ignore) {
            try {
                var m = c.getClass().getMethod("setSolde_net", Integer.class);
                m.invoke(c, value);
            } catch (Exception e) {
                throw new IllegalStateException("Client: setter soldeNet/solde_net introuvable");
            }
        }
    }
    private String getClientEmail(Client c) {
        try {
            var m = c.getClass().getMethod("getEmail"); // adapte si ton getter diffère
            Object v = m.invoke(c);
            return v == null ? null : String.valueOf(v);
        } catch (Exception e) {
            throw new IllegalStateException("Client: getter getEmail() introuvable");
        }
    }
    private boolean isPrepayee(Client c) {
        Object type = getTypeCompte(c);
        return type != null && "PREPAYE".equalsIgnoreCase(String.valueOf(type));
    }

    private String getTypeCompteString(Client c) {
        Object t = getTypeCompte(c);
        return t == null ? "INCONNU" : String.valueOf(t);
    }

}
