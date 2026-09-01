package com.book_store.user_service.service;

import com.book_store.user_service.entities.RevokedToken;
import com.book_store.user_service.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Server-side JWT revocation ("blacklist") store, backed by the database.
 * <p>
 * JWTs are stateless by design, so "logout" cannot simply delete a token -
 * instead we record its unique {@code jti} here. The custom {@link
 * org.springframework.security.oauth2.jwt.JwtDecoder} bean consults this
 * store on every incoming request and rejects any token whose jti is present.
 * <p>
 * Entries are only needed until the token's natural expiry, after which
 * {@link #purgeExpiredEntries()} removes them so the table doesn't grow forever.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenBlacklistService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void revokeToken(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            throw new IllegalArgumentException("Token has no 'jti' claim and cannot be individually revoked");
        }
        if (revokedTokenRepository.existsByJti(jti)) {
            log.debug("Token already revoked (jti={})", jti);
            return; // idempotent - logging out twice with the same token is not an error
        }
        Instant effectiveExpiry = expiresAt != null ? expiresAt : Instant.now().plusSeconds(3600);
        revokedTokenRepository.save(new RevokedToken(jti, effectiveExpiry, Instant.now()));
        log.info("Token revoked (jti={})", jti);
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsByJti(jti);
    }

    /** Purges blacklist rows for tokens that would already be rejected by expiry validation anyway. */
    @Scheduled(fixedRate = 3_600_000) // every hour
    public void purgeExpiredEntries() {
        revokedTokenRepository.deleteAllExpiredBefore(Instant.now());
        log.debug("Purged expired revoked-token entries");
    }
}

