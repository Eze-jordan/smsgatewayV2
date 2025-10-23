package com.ogooueTech.smsgateway.repository;

import com.ogooueTech.smsgateway.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByOriginalName(String originalName);
    void deleteByOriginalName(String originalName);
}
