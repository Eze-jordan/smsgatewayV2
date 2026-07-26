package com.ogooueTech.smsgateway.exception;

/**
 * Exception levée lorsqu'aucun manager ne correspond
 * aux informations recherchées.
 */
public class ManagerNotFoundException extends RuntimeException {

    public ManagerNotFoundException(String message) {
        super(message);
    }
}