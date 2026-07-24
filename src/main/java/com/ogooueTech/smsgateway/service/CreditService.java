package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.dtos.CreditRequestDto;
import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.enums.TypeCompte;
import com.ogooueTech.smsgateway.model.Client;
import com.ogooueTech.smsgateway.model.CreditRequest;
import com.ogooueTech.smsgateway.repository.ClientRepository;
import com.ogooueTech.smsgateway.repository.CreditRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Service chargé de gérer les demandes de crédit SMS.
 *
 * Le fonctionnement suit le principe Maker/Checker :
 * - le Maker crée une demande ;
 * - le Checker valide ou rejette la demande ;
 * - le Maker ne doit pas traiter sa propre demande.
 */
@Service
@Transactional
public class CreditService {

    private static final int ESTIMATED_AMOUNT_SCALE = 0;

    private final CreditRequestRepository creditRepository;
    private final ClientRepository clientRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    /**
     * Injection des dépendances.
     *
     * @param creditRepository repository des demandes de crédit
     * @param clientRepository repository des clients
     * @param notificationService service d'envoi des notifications
     * @param entityManager gestionnaire JPA utilisé pour verrouiller une demande
     */
    public CreditService(
            CreditRequestRepository creditRepository,
            ClientRepository clientRepository,
            NotificationService notificationService,
            EntityManager entityManager
    ) {
        this.creditRepository = creditRepository;
        this.clientRepository = clientRepository;
        this.notificationService = notificationService;
        this.entityManager = entityManager;
    }

    /**
     * Crée une demande de crédit pour un compte prépayé.
     *
     * Un verrou pessimiste est appliqué sur le client afin d'éviter
     * la création simultanée de demandes concurrentes.
     *
     * @param clientId identifiant du client
     * @param quantity quantité de SMS demandée
     * @param idempotencyKey clé permettant d'éviter les doublons
     * @return représentation DTO de la demande
     */
    public CreditRequestDto create(
            String clientId,
            int quantity,
            String idempotencyKey
    ) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId requis");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "La quantité doit être strictement supérieure à zéro"
            );
        }

        String normalizedClientId = clientId.trim();

        /*
         * Verrouille le client pendant toute la création.
         * Cela évite que deux requêtes concurrentes créent plusieurs
         * demandes pour le même client au même moment.
         */
        Client client = clientRepository.lockById(normalizedClientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client introuvable"
                ));

        assertPrepaidClient(client);

        String normalizedIdempotencyKey;

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            /*
             * En l'absence de clé fournie par le client, on retourne
             * la demande en attente existante, lorsqu'elle existe.
             */
            CreditRequest existingPending = creditRepository
                    .findByClient_IdclientsAndStatus(
                            normalizedClientId,
                            CreditStatus.PENDING,
                            PageRequest.of(0, 1)
                    )
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (existingPending != null) {
                return toDtoWithEstimate(existingPending);
            }

            normalizedIdempotencyKey = generateIdempotencyKey();
        } else {
            normalizedIdempotencyKey = idempotencyKey.trim();

            CreditRequest existing = creditRepository
                    .findByClient_IdclientsAndIdempotencyKey(
                            normalizedClientId,
                            normalizedIdempotencyKey
                    )
                    .orElse(null);

            if (existing != null) {
                return toDtoWithEstimate(existing);
            }
        }

        BigDecimal unitPrice = requireSmsPrice(client);

        String makerEmail = client.getEmail();

        if (makerEmail == null || makerEmail.isBlank()) {
            throw new IllegalStateException(
                    "L'adresse e-mail du client n'est pas définie"
            );
        }

        CreditRequest request = new CreditRequest();

        request.setClient(client);
        request.setQuantity(quantity);
        request.setMakerEmail(makerEmail.trim());
        request.setIdempotencyKey(normalizedIdempotencyKey);
        request.setStatus(CreditStatus.PENDING);

        CreditRequest savedRequest = creditRepository.save(request);

        BigDecimal estimatedAmount = calculateEstimatedAmount(
                unitPrice,
                quantity
        );

        notificationService.envoyerDemandeCredit(
                client,
                quantity,
                savedRequest.getRequestCode()
        );

        return CreditRequestDto.from(
                savedRequest,
                unitPrice,
                estimatedAmount
        );
    }

    /**
     * Approuve une demande et crédite le solde du client.
     *
     * La demande et le client sont verrouillés pour empêcher
     * une double validation concurrente.
     *
     * @param requestId identifiant UUID de la demande
     * @param checkerEmail adresse e-mail du validateur
     * @return demande après approbation
     */
    public CreditRequestDto approve(
            UUID requestId,
            String checkerEmail
    ) {
        String normalizedCheckerEmail = requireCheckerEmail(checkerEmail);

        /*
         * Verrouille directement la ligne de la demande.
         * Sans ce verrou, deux validateurs pourraient créditer
         * le même compte simultanément.
         */
        CreditRequest request = lockCreditRequest(requestId);

        assertPending(request);
        assertDifferentMakerAndChecker(request, normalizedCheckerEmail);
        assertValidQuantity(request);

        Client client = clientRepository.lockById(
                        request.getClient().getIdclients()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client introuvable"
                ));

        assertPrepaidClient(client);

        int previousBalance = client.getSoldeNet() == null
                ? 0
                : client.getSoldeNet();

        int creditedQuantity = request.getQuantity();
        int newBalance;

        try {
            newBalance = Math.addExact(
                    previousBalance,
                    creditedQuantity
            );
        } catch (ArithmeticException exception) {
            throw new IllegalStateException(
                    "Le nouveau solde dépasse la valeur maximale autorisée",
                    exception
            );
        }

        client.setSoldeNet(newBalance);
        client.setLastSoldeNet(creditedQuantity);

        clientRepository.save(client);

        request.setStatus(CreditStatus.APPROVED);
        request.setCheckerEmail(normalizedCheckerEmail);
        request.setValidatedAt(LocalDateTime.now());

        creditRepository.save(request);

        notificationService.envoyerCreditApprouve(
                client,
                creditedQuantity,
                newBalance
        );

        return CreditRequestDto.from(request);
    }

    /**
     * Rejette une demande sans modifier le solde du client.
     *
     * @param requestId identifiant UUID de la demande
     * @param checkerEmail adresse e-mail du validateur
     * @param reason motif du rejet
     * @return demande après rejet
     */
    public CreditRequestDto reject(
            UUID requestId,
            String checkerEmail,
            String reason
    ) {
        String normalizedCheckerEmail = requireCheckerEmail(checkerEmail);

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Le motif du rejet est obligatoire"
            );
        }

        CreditRequest request = lockCreditRequest(requestId);

        assertPending(request);
        assertDifferentMakerAndChecker(request, normalizedCheckerEmail);

        String normalizedReason = reason.trim();

        request.setStatus(CreditStatus.REJECTED);
        request.setCheckerEmail(normalizedCheckerEmail);
        request.setRejectReason(normalizedReason);
        request.setValidatedAt(LocalDateTime.now());

        creditRepository.save(request);

        notificationService.envoyerCreditRejete(
                request.getClient(),
                request.getQuantity(),
                normalizedReason
        );

        return CreditRequestDto.from(request);
    }

    /**
     * Retourne les demandes de crédit avec pagination.
     *
     * Les filtres client et statut sont facultatifs.
     *
     * @param clientId identifiant facultatif du client
     * @param status statut facultatif
     * @param pageable paramètres de pagination et de tri
     * @return page de demandes converties en DTO
     */
    @Transactional(readOnly = true)
    public Page<CreditRequestDto> list(
            String clientId,
            CreditStatus status,
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Les paramètres de pagination sont obligatoires"
            );
        }

        Page<CreditRequest> requests;

        if (clientId != null && !clientId.isBlank() && status != null) {
            requests = creditRepository.findByClient_IdclientsAndStatus(
                    clientId.trim(),
                    status,
                    pageable
            );
        } else if (clientId != null && !clientId.isBlank()) {
            requests = creditRepository.findByClient_Idclients(
                    clientId.trim(),
                    pageable
            );
        } else if (status != null) {
            requests = creditRepository.findByStatus(
                    status,
                    pageable
            );
        } else {
            requests = creditRepository.findAll(pageable);
        }

        return requests.map(CreditRequestDto::from);
    }

    /**
     * Verrouille une demande de crédit en écriture.
     *
     * @param requestId identifiant de la demande
     * @return demande verrouillée
     */
    private CreditRequest lockCreditRequest(UUID requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de la demande est obligatoire"
            );
        }

        CreditRequest request = entityManager.find(
                CreditRequest.class,
                requestId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (request == null) {
            throw new IllegalArgumentException(
                    "Demande de crédit introuvable"
            );
        }

        return request;
    }

    /**
     * Vérifie que la demande est toujours en attente.
     *
     * @param request demande à vérifier
     */
    private void assertPending(CreditRequest request) {
        if (request.getStatus() != CreditStatus.PENDING) {
            throw new IllegalStateException(
                    "La demande n'est plus en attente"
            );
        }
    }

    /**
     * Empêche le créateur de valider ou de rejeter sa propre demande.
     *
     * @param request demande traitée
     * @param checkerEmail adresse e-mail du validateur
     */
    private void assertDifferentMakerAndChecker(
            CreditRequest request,
            String checkerEmail
    ) {
        String makerEmail = request.getMakerEmail();

        if (makerEmail != null
                && makerEmail.equalsIgnoreCase(checkerEmail)) {
            throw new IllegalArgumentException(
                    "Le créateur ne peut pas traiter sa propre demande"
            );
        }
    }

    /**
     * Vérifie la quantité enregistrée dans la demande.
     *
     * @param request demande à vérifier
     */
    private void assertValidQuantity(CreditRequest request) {
        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "La quantité de crédit est invalide"
            );
        }
    }

    /**
     * Vérifie que le compte est prépayé.
     *
     * @param client client concerné
     */
    private void assertPrepaidClient(Client client) {
        if (client.getTypeCompte() != TypeCompte.PREPAYE) {
            throw new IllegalStateException(
                    "Le crédit est réservé aux comptes prépayés"
            );
        }
    }

    /**
     * Retourne le prix TTC d'un SMS.
     *
     * @param client client concerné
     * @return prix TTC du SMS
     */
    private BigDecimal requireSmsPrice(Client client) {
        BigDecimal unitPrice = client.getCoutSmsTtc();

        if (unitPrice == null) {
            throw new IllegalStateException(
                    "Le coût TTC d'un SMS n'est pas défini"
            );
        }

        if (unitPrice.signum() < 0) {
            throw new IllegalStateException(
                    "Le coût TTC d'un SMS ne peut pas être négatif"
            );
        }

        return unitPrice;
    }

    /**
     * Calcule le montant estimé de la demande.
     *
     * @param unitPrice prix unitaire
     * @param quantity quantité demandée
     * @return montant estimé
     */
    private BigDecimal calculateEstimatedAmount(
            BigDecimal unitPrice,
            int quantity
    ) {
        return unitPrice
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(
                        ESTIMATED_AMOUNT_SCALE,
                        RoundingMode.HALF_UP
                );
    }

    /**
     * Convertit une demande existante en DTO avec son estimation.
     *
     * @param request demande existante
     * @return DTO de la demande
     */
    private CreditRequestDto toDtoWithEstimate(
            CreditRequest request
    ) {
        assertValidQuantity(request);

        BigDecimal unitPrice = requireSmsPrice(request.getClient());

        BigDecimal estimatedAmount = calculateEstimatedAmount(
                unitPrice,
                request.getQuantity()
        );

        return CreditRequestDto.from(
                request,
                unitPrice,
                estimatedAmount
        );
    }

    /**
     * Valide et normalise l'adresse e-mail du Checker.
     *
     * @param checkerEmail adresse fournie
     * @return adresse normalisée
     */
    private String requireCheckerEmail(String checkerEmail) {
        if (checkerEmail == null || checkerEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "L'adresse e-mail du validateur est obligatoire"
            );
        }

        return checkerEmail.trim();
    }

    /**
     * Génère une clé d'idempotence suffisamment difficile
     * à reproduire accidentellement.
     *
     * UUID est préférable à Math.random(), qui générait auparavant
     * seulement six chiffres et présentait davantage de collisions.
     *
     * @return nouvelle clé d'idempotence
     */
    private String generateIdempotencyKey() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase(Locale.ROOT);

        return "REQ-" + randomPart;
    }
}