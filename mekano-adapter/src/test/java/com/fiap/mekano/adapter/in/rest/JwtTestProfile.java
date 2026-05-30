package com.fiap.mekano.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * QuarkusTestProfile que gera um par de chaves RSA-2048 in-memory para os
 * testes JWT da Phase 8 (D-10 / RESEARCH §D-10).
 *
 * Por que inline (`mp.jwt.verify.publickey`) e não `mp.jwt.verify.publickey.location`:
 *   - O bloco `application.properties` (plano 08-02) aponta para o arquivo
 *     `publicKey.pem` no classpath. Para os testes desta fase precisamos
 *     emitir tokens com uma chave privada conhecida em CI — sem materializar
 *     PEM em disco. A propriedade inline `mp.jwt.verify.publickey` toma
 *     precedência sobre `.location` em runtime (RESEARCH §A3) e nos permite
 *     injetar o PEM correspondente ao `KEY_PAIR` deste profile.
 *   - Resultado: pipeline CI roda sem chave estática commitada (gitignore
 *     do plano 08-01) e sem dependência de filesystem.
 *
 * <p>Bridge classloader (deviation Rule 1, 08-05): Quarkus carrega
 * {@link QuarkusTestProfile} no launcher classloader e o test class no
 * application classloader; o campo {@code static KEY_PAIR} acessado a
 * partir do teste seria uma instância diferente daquela usada em
 * {@link #getConfigOverrides()}. Para evitar isso, exportamos a chave
 * privada como system property base64-PKCS8 — System properties são
 * JVM-wide, atravessando classloaders. {@link #privateKey()} reconstrói
 * a {@link PrivateKey} no classloader do teste.
 *
 * Referência: D-10 — chave gerada em memória, sem chave em disco para testes.
 */
public class JwtTestProfile implements QuarkusTestProfile {

    /** System property keys para bridge entre classloaders (Rule 1). */
    public static final String PRIVATE_KEY_PROP = "mekano.test.jwt.private-key.b64";
    public static final String PUBLIC_KEY_PEM_PROP = "mekano.test.jwt.public-key.pem";

    private static final String PUBLIC_KEY_PEM;

    static {
        try {
            // Quarkus carrega QuarkusTestProfile no launcher classloader e o test class
            // no application classloader; ambos rodam ESTE static block independentemente.
            // Para garantir que pub e priv sejam matched pair, primeiro classloader a
            // chegar gera o KP e publica via System properties (JVM-wide); o segundo
            // detecta e reusa. Sincronização via classe `String.class` (sempre carregada
            // no bootstrap classloader, único entre todos os filhos).
            synchronized (String.class) {
                String existingPem = System.getProperty(PUBLIC_KEY_PEM_PROP);
                String existingPriv = System.getProperty(PRIVATE_KEY_PROP);
                if (existingPem != null && existingPriv != null) {
                    PUBLIC_KEY_PEM = existingPem;
                } else {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                    generator.initialize(2048);
                    KeyPair kp = generator.generateKeyPair();

                    byte[] pubEncoded = kp.getPublic().getEncoded();
                    String pubBase64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(pubEncoded);
                    PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\n" + pubBase64 + "\n-----END PUBLIC KEY-----";

                    String privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
                    System.setProperty(PUBLIC_KEY_PEM_PROP, PUBLIC_KEY_PEM);
                    System.setProperty(PRIVATE_KEY_PROP, privB64);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar par RSA para JwtTestProfile", e);
        }
    }

    /**
     * Reconstrói a chave privada a partir da system property publicada pelo
     * static initializer. Chamada pelo {@code UserResourceJwtTest} para
     * assinar tokens com {@code Jwt.sign(PrivateKey)}.
     */
    public static PrivateKey privateKey() {
        try {
            String b64 = System.getProperty(PRIVATE_KEY_PROP);
            if (b64 == null) {
                throw new IllegalStateException(
                        "JwtTestProfile.PRIVATE_KEY_PROP ausente — @TestProfile(JwtTestProfile.class) não aplicado?");
            }
            byte[] der = Base64.getDecoder().decode(b64);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao reconstruir PrivateKey do bridge", e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        try {
            // Write the test public key to a temp file and point the runtime config there.
            // Inline `mp.jwt.verify.publickey` SHOULD take precedence over `.location`,
            // but Quarkus 3.36 emits SRJWT03007 ("Public key is configured but either
            // the secret key or key location are also configured and will be ignored")
            // and in practice falls back to the static publicKey.pem on classpath —
            // signature verify then fails for tokens we sign here. Overriding
            // `.location` with the temp file is unambiguous.
            java.nio.file.Path pem = java.nio.file.Files.createTempFile("jwt-test-pub", ".pem");
            java.nio.file.Files.writeString(pem, PUBLIC_KEY_PEM);
            pem.toFile().deleteOnExit();
            return Map.of(
                    "smallrye.jwt.verify.key.location", pem.toAbsolutePath().toString().replace('\\', '/'),
                    "mp.jwt.verify.publickey.location", pem.toAbsolutePath().toString().replace('\\', '/')
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao publicar chave pública de teste", e);
        }
    }
}

