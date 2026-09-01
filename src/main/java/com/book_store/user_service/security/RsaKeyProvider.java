package com.book_store.user_service.security;

import lombok.Getter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Component
@Getter
public class RsaKeyProvider {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String keyId;

    public RsaKeyProvider(@Value("${jwt.rsa.private-key:}") String privateKeyBase64,
                          @Value("${jwt.rsa.public-key:}") String publicKeyBase64,
                          @Value("${jwt.rsa.key-id:user-service-key-1}") String keyId) throws NoSuchAlgorithmException {

        if (StringUtils.hasText(privateKeyBase64) && StringUtils.hasText(publicKeyBase64)) {
            this.privateKey = loadPrivateKey(privateKeyBase64);
            this.publicKey = loadPublicKey(publicKeyBase64);
            this.keyId = keyId;
            logger.info("RSA keys loaded successfully. Key ID: {}", keyId);
        } else {
            logger.warn("!!! No RSA key pair configured (jwt.rsa.private-key / jwt.rsa.public-key) — " +
                    "generating an EPHEMERAL key pair for local development only. " +
                    "Tokens will become invalid on every restart and other services " +
                    "cannot validate them without sharing this in-memory key. " +
                    "Configure real keys before deploying.");

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keyPair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.keyId = "ephemeral-" + UUID.randomUUID();
        }
    }

    private RSAPublicKey loadPublicKey(String publicKeyBase64) {
        byte[] bytes = Base64.getDecoder().decode(publicKeyBase64);
        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Error loading public key", e);
            throw new RuntimeException("Error loading public key", e);
        }
    }

    private RSAPrivateKey loadPrivateKey(String privateKeyBase64) {
        byte[] bytes = Base64.getDecoder().decode(privateKeyBase64);
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(spec);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Error loading private key", e);
            throw new RuntimeException("Error loading private key", e);
        }
    }
}
