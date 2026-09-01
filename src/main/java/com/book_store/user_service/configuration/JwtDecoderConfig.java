package com.book_store.user_service.configuration;

import com.book_store.user_service.service.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Overrides the default auto-configured {@link JwtDecoder} to additionally
 * reject tokens whose "jti" claim has been revoked (see {@link TokenBlacklistService}).
 * <p>
 * Spring Boot's oauth2-resource-server auto-configuration only creates its own
 * {@code JwtDecoder} bean when none is already present ({@code @ConditionalOnMissingBean}),
 * so defining this bean here fully replaces it while still using the same JWKS URI.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            TokenBlacklistService tokenBlacklistService) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefault(); // exp/nbf checks
        OAuth2TokenValidator<Jwt> notRevokedValidator = token -> {
            String jti = token.getId(); // maps to the "jti" claim
            if (jti != null && tokenBlacklistService.isRevoked(jti)) {
                OAuth2Error error = new OAuth2Error(
                        "token_revoked",
                        "Token has been revoked (user logged out)",
                        null);
                return OAuth2TokenValidatorResult.failure(error);
            }
            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, notRevokedValidator));
        return decoder;
    }
}

