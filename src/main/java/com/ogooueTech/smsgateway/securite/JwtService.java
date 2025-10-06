package com.ogooueTech.smsgateway.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey secretKey = Keys.hmacShaKeyFor(
            "0cee2891cfeffdf9057548357fc32584c5243b15f1f96d808a86f5eacfb17d88d".getBytes(StandardCharsets.UTF_8)
    );

    // ============================
    // 🔹 EXTRACTION
    // ============================

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public String extractId(String token) { return extractClaim(token, "id"); }
    public String extractTypeCompte(String token) { return extractClaim(token, "typeCompte"); }
    public String extractRole(String token) { return extractClaim(token, "role"); }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private String extractClaim(String token, String key) {
        return extractAllClaims(token).get(key, String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ============================
    // 🔹 GÉNÉRATION DU TOKEN
    // ============================

    public String generateToken(CustomUserDetails userDetails,
                                String nom,
                                String email,
                                String role,
                                String typeCompte) {

        return Jwts.builder()
                .setSubject(email)
                .claim("id", userDetails.getId())
                .claim("nom", nom)
                .claim("role", role)
                .claim("typeCompte", typeCompte)
                .claim("statutCompte", userDetails.getStatutCompte())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 heures
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ============================
    // 🔹 VALIDATION
    // ============================

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ============================
    // 🔹 TOKEN RESET MOT DE PASSE
    // ============================

    public String generatePasswordResetToken(String subject, int expiresInMinutes) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .claim("typ", "pwd_reset")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiresInMinutes * 60_000L))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isPasswordResetTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object typ = claims.get("typ");
            if (typ == null || !"pwd_reset".equals(typ.toString())) return false;
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractSubjectFromPasswordResetToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public void assertValidPasswordResetToken(String token) {
        Claims claims = extractAllClaims(token);
        Object typ = claims.get("typ");
        if (typ == null || !"pwd_reset".equals(typ.toString())) {
            throw new IllegalArgumentException("Type de jeton invalide");
        }
        if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
            throw new IllegalArgumentException("Jeton expiré");
        }
    }
}
