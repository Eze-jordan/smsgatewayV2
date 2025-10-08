package com.ogooueTech.smsgateway.config;


import com.ogooueTech.smsgateway.securite.JwtFiller;
import com.ogooueTech.smsgateway.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
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

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtFiller jwtFiller;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          BCryptPasswordEncoder bCryptPasswordEncoder,
                          JwtFiller jwtFiller) {
        this.customUserDetailsService = customUserDetailsService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.jwtFiller = jwtFiller;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/V1/managers/create"
                        ).permitAll()
                        .requestMatchers(
                                "/api/V1/managers/activation",
                                "/api/V1/managers/resend-otp",
                                "/api/V1/auth/**",
                                "/api/V1/password/forgot",
                                "/api/V1/password/reset",
                                "/api/V1/manager/password/forgot",
                                "/api/V1/manager/password/reset",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFiller, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ On autorise plusieurs domaines + wildcard (*)
        configuration.addAllowedOriginPattern("http://localhost:3001");
        configuration.addAllowedOriginPattern("http://localhost:4200");
        configuration.addAllowedOriginPattern("*"); // tous les domaines externes autorisés

        // ✅ Autoriser toutes les méthodes HTTP
        configuration.setAllowedMethods(Arrays.asList("*"));

        // ✅ Autoriser tous les headers
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // ✅ Exposer certains headers dans la réponse (visibles côté client)
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // ✅ Autoriser les cookies / tokens JWT
        configuration.setAllowCredentials(true);

        // ✅ Mise en cache de la politique CORS (1h)
        configuration.setMaxAge(3600L);

        // ✅ Appliquer la config à toutes les routes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(bCryptPasswordEncoder);
        return provider;
    }
}
