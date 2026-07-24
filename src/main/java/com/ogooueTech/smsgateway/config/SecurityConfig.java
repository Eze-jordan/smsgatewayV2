package com.ogooueTech.smsgateway.config;

import com.ogooueTech.smsgateway.securite.ApiKeyFilter;
import com.ogooueTech.smsgateway.securite.JwtFiller;
import com.ogooueTech.smsgateway.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtFiller jwtFiller;
    private final ApiKeyFilter apiKeyFilter;

    /*
     * Les valeurs CORS sont maintenant chargées depuis
     * application.properties, lui-même alimenté par les
     * variables d'environnement de la VM.
     */
    private final String allowedOriginPatterns;
    private final String allowedMethods;
    private final String allowedHeaders;
    private final String exposedHeaders;
    private final boolean allowCredentials;
    private final long maxAge;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            JwtFiller jwtFiller,
            ApiKeyFilter apiKeyFilter,
            @Value("${app.security.cors.allowed-origin-patterns}")
            String allowedOriginPatterns,
            @Value("${app.security.cors.allowed-methods}")
            String allowedMethods,
            @Value("${app.security.cors.allowed-headers}")
            String allowedHeaders,
            @Value("${app.security.cors.exposed-headers}")
            String exposedHeaders,
            @Value("${app.security.cors.allow-credentials}")
            boolean allowCredentials,
            @Value("${app.security.cors.max-age}")
            long maxAge
    ) {
        this.customUserDetailsService = customUserDetailsService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtFiller = jwtFiller;
        this.apiKeyFilter = apiKeyFilter;
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
        this.exposedHeaders = exposedHeaders;
        this.allowCredentials = allowCredentials;
        this.maxAge = maxAge;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> {
                                    response.sendError(
                                            HttpServletResponse.SC_UNAUTHORIZED,
                                            "Unauthorized"
                                    );
                                }
                        )
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/V1/managers/create"
                        ).permitAll()
                        .requestMatchers(
                                "/api/V1/documents/**",
                                "/api/V1/managers/activation",
                                "/api/V1/managers/resend-otp",
                                "/api/V1/auth/**",
                                "/api/V1/password/forgot",
                                "/api/V1/password/reset",
                                "/api/V1/manager/password/forgot",
                                "/api/V1/manager/password/reset",
                                "/api/V1/sms/{ref}/mark-sent",
                                "/api/V1/sms/pending",
                                "/swagger-ui/**",
                                "/send-test-email",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/V1/sms/unides",
                                "/api/V1/sms/muldes",
                                "/api/V1/sms/muldesp"
                        ).permitAll()

                        .requestMatchers(
                                "/api/V1/sms/unides",
                                "/api/V1/sms/muldes",
                                "/api/V1/sms/muldesp"
                        ).authenticated()
                        .anyRequest()
                        .authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .addFilterBefore(
                        apiKeyFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        jwtFiller,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

  @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Chaque variable peut contenir plusieurs valeurs
         * séparées par des virgules.
         */
        configuration.setAllowedOriginPatterns(
                convertirEnListe(allowedOriginPatterns)
        );

        configuration.setAllowedMethods(
                convertirEnListe(allowedMethods)
        );

        configuration.setAllowedHeaders(
                convertirEnListe(allowedHeaders)
        );

        configuration.setExposedHeaders(
                convertirEnListe(exposedHeaders)
        );

        configuration.setAllowCredentials(
                allowCredentials
        );

        configuration.setMaxAge(
                maxAge
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    /**
     * Transforme une chaîne séparée par des virgules
     * en liste de valeurs nettoyées.
     */
    private List<String> convertirEnListe(String values) {
        if (values == null || values.isBlank()) {
            return List.of();
        }

        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(
                customUserDetailsService
        );

        provider.setPasswordEncoder(
                bCryptPasswordEncoder
        );

        return provider;
    }
}