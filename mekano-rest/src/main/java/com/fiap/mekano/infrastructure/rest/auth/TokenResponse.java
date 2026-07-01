package com.fiap.mekano.infrastructure.rest.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fiap.mekano.domain.port.in.TokenPair;

public record TokenResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn
) {
    public static TokenResponse from(TokenPair tokenPair) {
        return new TokenResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.expiresIn()
        );
    }
}
