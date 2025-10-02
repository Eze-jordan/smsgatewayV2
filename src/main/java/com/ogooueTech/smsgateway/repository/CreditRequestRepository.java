package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.CreditStatus;
import com.ogooueTech.smsgateway.model.CreditRequest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditRequestRepository extends JpaRepository<CreditRequest, UUID> {

    Optional<CreditRequest> findByClient_IdclientsAndIdempotencyKey(String clientId, String idempotencyKey);

    Page<CreditRequest> findByClient_Idclients(String clientId, Pageable pageable);

    Page<CreditRequest> findByClient_IdclientsAndStatus(String clientId, CreditStatus status, Pageable pageable);

    Page<CreditRequest> findByStatus(CreditStatus status, Pageable pageable);
}
