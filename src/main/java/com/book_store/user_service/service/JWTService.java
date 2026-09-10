package com.book_store.user_service.service;

import com.book_store.user_service.entities.User;
import com.book_store.user_service.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JWTService {

    private final SecretKey secretKey;
    private final long expirationTime; // in milliseconds -> from application properties

    private final RsaKeyProvider rsaKeyProvider;
    public JWTService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationTime, RsaKeyProvider rsaKeyProvider) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
        this.rsaKeyProvider = rsaKeyProvider;
    }

    public String generateTokenUsingHmac(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationTime);

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole(),
                "name", user.getFirst_name(),
                "email", user.getEmail()
        );

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(java.util.Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateTokenUsingRSA(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationTime);

        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "role", user.getRole(),
                "name", user.getFirst_name(),
                "email", user.getEmail()
        );

        return Jwts.builder()
                .header().keyId(rsaKeyProvider.getKeyId()).and()
                .id(UUID.randomUUID().toString()) // "jti" claim - required so logout can revoke this specific token
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
