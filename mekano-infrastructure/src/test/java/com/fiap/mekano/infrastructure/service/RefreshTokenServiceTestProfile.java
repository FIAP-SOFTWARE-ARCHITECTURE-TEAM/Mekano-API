package com.fiap.mekano.infrastructure.service;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

/**
 * QuarkusTestProfile que gera um par de chaves RSA-2048 in-memory para
 * {@link RefreshTokenServiceTest}.
 *
 * <p>A chave privada é exportada como PEM PKCS#8 para um arquivo temporário
 * e configurada via {@code smallrye.jwt.sign.key.location} — necessária para
 * que {@code Jwt.issuer(...)} consiga assinar tokens sem uma chave explicitamente
 * passada.
 *
 * <p>Nenhuma chave é commitada no repositório (gitignore: {@code privateKey*.pem}).
 */
public class RefreshTokenServiceTestProfile implements QuarkusTestProfile {

    private static final String SIGN_KEY_LOCATION;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair kp = generator.generateKeyPair();

            // Exporta chave privada como PEM PKCS#8
            byte[] privDer = kp.getPrivate().getEncoded();
            String privB64 = Base64.getMimeEncoder(64, "\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                    .encodeToString(privDer);
            String privPem = "-----BEGIN PRIVATE KEY-----\n" + privB64 + "\n-----END PRIVATE KEY-----";

            java.nio.file.Path privPath = java.nio.file.Files.createTempFile("refresh-test-priv", ".pem");
            java.nio.file.Files.writeString(privPath, privPem);
            privPath.toFile().deleteOnExit();
            SIGN_KEY_LOCATION = privPath.toAbsolutePath().toString().replace('\\', '/');
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar par RSA para RefreshTokenServiceTestProfile", e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "smallrye.jwt.sign.key.location", SIGN_KEY_LOCATION
        );
    }
}
