package com.book_store.user_service.service;

import com.book_store.user_service.entities.User;
import com.book_store.user_service.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JWTService {

    @Getter
    private final long expirationTime; // in milliseconds -> from application properties

    private final RsaKeyProvider rsaKeyProvider;
    private final String issuer;
    private final List<String> audiences;

    public JWTService(@Value("${jwt.expiration}") long expirationTime,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.audiences}") List<String> audiences,
                      RsaKeyProvider rsaKeyProvider) {
        this.expirationTime = expirationTime;
        this.issuer = issuer;
        this.audiences = audiences;
        this.rsaKeyProvider = rsaKeyProvider;
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
                .issuer(issuer)
                .audience().add(audiences).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(rsaKeyProvider.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

}
