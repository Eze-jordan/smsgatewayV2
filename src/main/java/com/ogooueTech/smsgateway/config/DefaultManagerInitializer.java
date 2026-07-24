package com.ogooueTech.smsgateway.config;

import com.ogooueTech.smsgateway.service.DefaultManagerBootstrapService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Lance automatiquement la vérification du manager
 * par défaut à chaque démarrage de l'application.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultManagerInitializer implements ApplicationRunner {

    private final DefaultManagerBootstrapService bootstrapService;

    public DefaultManagerInitializer(
            DefaultManagerBootstrapService bootstrapService
    ) {
        this.bootstrapService = bootstrapService;
    }

    /**
     * Méthode exécutée après l'initialisation
     * du contexte Spring.
     */
    @Override
    public void run(ApplicationArguments args) {
        bootstrapService.ensureDefaultManagerExists();
    }
}