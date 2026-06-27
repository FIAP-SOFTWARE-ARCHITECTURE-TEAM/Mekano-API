package com.fiap.mekano.infrastructure.rest.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LogoutRequest(
        @JsonProperty("refresh_token")
        String refreshToken
) {
}
