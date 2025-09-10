package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Manager;
import com.ogooueTech.smsgateway.model.Validation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidationRipository extends JpaRepository<Validation, String> {

    Optional<Validation> findByCode(String code);
    Optional<Validation> findByManager(Manager manager);

}
