package com.book_store.user_service.controllers;

import com.book_store.user_service.security.RsaKeyProvider;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Downstream services can retrieve the public key from this endpoint to validate JWTs issued by this service.
 * The endpoint is configured in application.yml for the resource server:
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           jwk-set-uri: http://user-service:8081/.well-known/jwks.json
 */
@RestController
public class JwksSetController {
    private final RsaKeyProvider rsaKeyProvider;

    public JwksSetController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }


    /**
     * Endpoint to expose the JSON Web Key Set (JWKS) for public key retrieval.
     * This is used by clients to verify JWTs signed with the corresponding private key.
     *
     * @return A map representing the JWKS in JSON format.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> getJwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyProvider.getPublicKey())
                .keyUse(KeyUse.SIGNATURE)
                .keyID(rsaKeyProvider.getKeyId())
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .build();

//         true = public-only view (never exposes the private key material)
        return new JWKSet(rsaKey).toJSONObject(true);
    }
}
