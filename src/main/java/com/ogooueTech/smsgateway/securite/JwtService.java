package com.ogooueTech.smsgateway.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.cglib.core.internal.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // ✅ Clé secrète de 256 bits recommandée (doit être stockée en sécurité en prod)
    private final SecretKey secretKey = Keys.hmacShaKeyFor("0cee2891cfeffdf9057548357fc32584c5243b15f1f96d808a86f5eacfb17d88d".getBytes());

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(secretKey) // ✅ moderne
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(com.ogooueTech.smsgateway.securite.CustomUserDetails userDetails, String nom, String email, String role, boolean abonneExpire) {
        return Jwts.builder()
                .setSubject(email)
                .claim("id", userDetails.getId())
                .claim("nom", nom)
                .claim("role", role)
                .claim("abonneExpire", abonneExpire)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractId(String token) {
        return extractClaim(token, "id");
    }




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
    public String extractClaim(String token, String claimKey) {
        return extractAllClaims(token).get(claimKey, String.class);
    }

    // ======= AJOUTER DANS JwtService =======

    /** Génère un JWT court pour réinitialisation de mot de passe.
     *  - subject: clientId (ou email si tu préfères)
     *  - claim 'typ': 'pwd_reset'
     *  - expiration en minutes (ex: 30)
     */
    public String generatePasswordResetToken(String subject /* clientId ou email */, int expiresInMinutes) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .claim("typ", "pwd_reset")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiresInMinutes * 60_000L))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Vérifie que le token est un jeton de reset valide (signature + typ + non expiré). */
    public boolean isPasswordResetTokenValid(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Vérifie le type
            Object typ = claims.get("typ");
            if (typ == null || !"pwd_reset".equals(typ.toString())) return false;

            // Vérifie l’expiration
            Date exp = claims.getExpiration();
            return exp != null && exp.after(new Date());
        } catch (Exception e) {
            return false; // signature invalide, token mal formé, expiré, etc.
        }
    }

    /** Extrait le "subject" (clientId ou email) du jeton de reset (sans autre vérif). */
    public String extractSubjectFromPasswordResetToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /** (Optionnel) Lève des exceptions détaillées si invalide, utile côté service. */
    public void assertValidPasswordResetToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object typ = claims.get("typ");
        if (typ == null || !"pwd_reset".equals(typ.toString())) {
            throw new IllegalArgumentException("Type de jeton invalide");
        }
        Date exp = claims.getExpiration();
        if (exp == null || !exp.after(new Date())) {
            throw new IllegalArgumentException("Jeton expiré");
        }
    }


}
