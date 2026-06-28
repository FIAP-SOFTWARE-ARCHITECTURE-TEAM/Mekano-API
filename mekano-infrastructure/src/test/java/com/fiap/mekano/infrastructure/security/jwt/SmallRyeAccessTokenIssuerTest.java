package com.fiap.mekano.infrastructure.security.jwt;

import com.fiap.mekano.domain.model.Role;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(SmallRyeAccessTokenIssuerTest.JwtTestProfile.class)
class SmallRyeAccessTokenIssuerTest {

    @Inject
    SmallRyeAccessTokenIssuer accessTokenIssuer;

    @Test
    void issue_deveGerarJwtComHeaderPayloadEClaimsEsperadas() {
        UUID userUuid = UUID.randomUUID();

        String token = accessTokenIssuer.issue(
                userUuid,
                "Admin Mekano",
                Role.admin
        );

        String[] parts = token.split("\\.");

        assertThat(parts).hasSize(3);

        String headerJson = decodeBase64Url(parts[0]);
        String payloadJson = decodeBase64Url(parts[1]);

        assertThat(headerJson)
                .contains("\"alg\":\"EdDSA\"")
                .contains("\"typ\":\"JWT\"");

        assertThat(payloadJson)
                .contains("\"iss\":\"mekano-api-test\"")
                .contains("\"sub\":\"" + userUuid + "\"")
                .contains("\"name\":\"Admin Mekano\"")
                .contains("\"groups\":[\"admin\"]");

        long issuedAt = extractLongClaim(payloadJson, "iat");
        long expiresAt = extractLongClaim(payloadJson, "exp");

        assertThat(expiresAt - issuedAt).isEqualTo(900L);
    }

    private static String decodeBase64Url(String value) {
        byte[] decoded = Base64.getUrlDecoder().decode(value);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static long extractLongClaim(String json, String claim) {
        var pattern = Pattern.compile("\"" + claim + "\":(\\d+)");
        var matcher = pattern.matcher(json);

        if (!matcher.find()) {
            throw new AssertionError("Claim numérica não encontrada: " + claim + ". Payload: " + json);
        }

        return Long.parseLong(matcher.group(1));
    }

    public static class JwtTestProfile implements QuarkusTestProfile {

        private static final String TEST_PRIVATE_KEY = """
                -----BEGIN PRIVATE KEY-----
                MC4CAQAwBQYDK2VwBCIEIB+dtWBdKE7vIeKOhgm4dgWETx74PdmJsAowoMp3ib8D
                -----END PRIVATE KEY-----
                """;

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "smallrye.jwt.sign.key", TEST_PRIVATE_KEY,
                    "smallrye.jwt.new-token.signature-algorithm", "eddsa",
                    "smallrye.jwt.new-token.issuer", "mekano-api-test",
                    "mekano.jwt.access-token-seconds", "900"
            );
        }
    }
}