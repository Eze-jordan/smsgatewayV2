package com.ogooueTech.smsgateway.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table (name = "validation")
public class Validation {
    @Id
    @Column(name = "id", nullable = false,length = 100,updatable = false)
    private String  id;
    private Instant creation;
    private Instant expiration;
    private Instant activation;
    private String code;
    @OneToOne
    @JoinColumn(name = "manager_id")
    private Manager manager;


    public Validation() {

    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Instant getActivation() {
        return activation;
    }

    public void setActivation(Instant activation) {
        this.activation = activation;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    public Instant getCreation() {
        return creation;
    }

    public void setCreation(Instant creation) {
        this.creation = creation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Validation(String id, Instant creation, Instant expiration, Instant activation, String code, Manager manager) {
        this.id = id;
        this.creation = creation;
        this.expiration = expiration;
        this.activation = activation;
        this.code = code;
        this.manager = manager;

    }
}
