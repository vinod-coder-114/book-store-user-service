package com.book_store.user_service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a JWT that has been explicitly revoked (e.g. via user logout)
 * before its natural expiration. Entries only need to be kept until
 * {@link #expiresAt}, after which the underlying token would be rejected
 * by expiry validation anyway and the row can be purged.
 */
@Entity
@Table(name = "revoked_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    /** The JWT's "jti" (JWT ID) claim - unique per issued token. */
    @Id
    @Column(name = "jti", nullable = false, unique = true)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;
}

