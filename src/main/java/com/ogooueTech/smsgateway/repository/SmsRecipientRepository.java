package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.enums.SmsStatus;
import com.ogooueTech.smsgateway.model.SmsRecipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SmsRecipientRepository extends JpaRepository<SmsRecipient, Long> {

    List<SmsRecipient> findBySms_RefAndStatut(String ref, SmsStatus statut);

    Page<SmsRecipient> findBySms_RefAndStatut(String ref, SmsStatus statut, Pageable pageable);

    boolean existsBySms_RefAndStatut(String ref, SmsStatus statut);


    List<SmsRecipient> findBySms_RefIn(List<String> refs);
    
}
