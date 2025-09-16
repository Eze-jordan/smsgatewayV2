package com.ogooueTech.smsgateway.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class FooterInfo {
    @Id
    private Long id = 1L; // Un seul enregistrement, ID fixe = 1

    private String companyName;
    private String companyAddress;
    private String companyNif;
    private String companyRccm;
    private String companyEmail;
    private String companyPhone;
    private String paymentNote;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyNif() {
        return companyNif;
    }

    public void setCompanyNif(String companyNif) {
        this.companyNif = companyNif;
    }

    public String getCompanyRccm() {
        return companyRccm;
    }

    public void setCompanyRccm(String companyRccm) {
        this.companyRccm = companyRccm;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getPaymentNote() {
        return paymentNote;
    }

    public void setPaymentNote(String paymentNote) {
        this.paymentNote = paymentNote;
    }
}
