package com.ashraf.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JWTUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value(("${jwt.expiration-ms}"))
    private long expirationMs;

    private SecretKey getSigningKey() {
        return  Keys.hmacShaKeyFor(secret.getBytes());
    }

    public  String generateToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims:: getSubject);
    }


    public boolean isTokenValid(String token) {
        try{
            extractAllClaims(token);
            return !isTokenExpired(token);
        }catch (JwtException | IllegalArgumentException e) {
            return  false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token,Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
