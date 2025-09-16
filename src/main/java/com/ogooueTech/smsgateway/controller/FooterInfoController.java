package com.ogooueTech.smsgateway.controller;

import com.ogooueTech.smsgateway.model.FooterInfo;
import com.ogooueTech.smsgateway.service.FooterInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/footer")
public class FooterInfoController {

    private final FooterInfoService service;

    public FooterInfoController(FooterInfoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FooterInfo> getFooter() {
        return ResponseEntity.ok(service.getFooterInfo());
    }

    @PutMapping
    public ResponseEntity<FooterInfo> updateFooter(@RequestBody FooterInfo body) {
        return ResponseEntity.ok(service.updateFooterInfo(body));
    }
}
