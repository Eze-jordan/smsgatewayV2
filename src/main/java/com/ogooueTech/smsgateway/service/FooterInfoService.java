package com.ogooueTech.smsgateway.service;

import com.ogooueTech.smsgateway.model.FooterInfo;
import com.ogooueTech.smsgateway.repository.FooterInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class FooterInfoService {

    private final FooterInfoRepository repository;

    public FooterInfoService(FooterInfoRepository repository) {
        this.repository = repository;
    }

    public FooterInfo getFooterInfo() {
        return repository.findById(1L).orElseGet(() -> {
            FooterInfo defaultInfo = new FooterInfo();
            defaultInfo.setId(1L);
            defaultInfo.setCompanyName("OgooueTech S.A.R.L");
            defaultInfo.setCompanyAddress("Libreville, Gabon");
            defaultInfo.setCompanyNif("123456789");
            defaultInfo.setCompanyRccm("GA-LBV-2025-B-00001");
            defaultInfo.setCompanyEmail("contact@ogoouetech.com");
            defaultInfo.setCompanyPhone("+241 01 23 45 67");
            defaultInfo.setPaymentNote("Merci de régler la facture sous 30 jours.");
            return repository.save(defaultInfo);
        });
    }


    public FooterInfo updateFooterInfo(FooterInfo newInfo) {
        newInfo.setId(1L); // On force toujours l’ID = 1
        return repository.save(newInfo);
    }
}
