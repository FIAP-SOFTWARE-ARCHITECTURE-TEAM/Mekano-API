package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 *  QuarkusTestProfile que gera um par de chaves Ed25519 in-memory para os
 *  testes JWT da Phase 9 (D-10 — ES256/EdDSA).
 *
 * Por que inline (`mp.jwt.verify.publickey`) e não `mp.jwt.verify.publickey.location`:
 *   - O bloco `application.properties` (plano 08-02) aponta para o arquivo
 *     `publicKey.pem` no classpath. Para os testes desta fase precisamos
 *     emitir tokens com uma chave privada conhecida em CI — sem materializar
 *     PEM em disco. A propriedade inline `mp.jwt.verify.publickey` toma
 *     precedência sobre `.location` em runtime (RESEARCH §A3) e nos permite
     *  injetar o PEM correspondente ao `KEY_PAIR` deste profile.
     *   - Ed25519 (Phase 9, D-10): substitui RSA-2048 usado na Phase 8.
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
 * <p><b>WR-05 (Code Review 08) — atenção operacional:</b> a chave privada
 * de teste é publicada como System property ({@link #PRIVATE_KEY_PROP}) e
 * portanto fica acessível a qualquer código no mesmo JVM. Embora seja uma
 * chave gerada apenas para a vida do JVM de teste (nunca tocando disco
 * persistente, nunca commitada), <b>NÃO logue {@code System.getProperties()}
 * em testes nem em código que rode na mesma JVM</b> — isso despejaria a
 * chave em logs de CI. Para diagnósticos, prefira logar chaves específicas
 * e mascarar {@code mekano.test.jwt.*}. A reestruturação para evitar o
 * canal System.properties (ex.: stash em singleton no bootstrap classpath)
 * é trabalho de v2 — depende de inversão do bridge launcher↔application.
 *
 * <p><b>WR-04 (Code Review 08) — choice of monitor:</b> a sincronização
 * cross-classloader é feita em {@code System.getProperties()} (objeto
 * único na JVM, alcançável dos dois classloaders) em vez de
 * {@code String.class}. {@code String.class} também funcionaria como
 * monitor JVM-wide, mas é compartilhado com JDK internals (intern table,
 * formatter caches, serialization shims) e qualquer biblioteca que use
 * o mesmo "truque", criando risco de contenção/deadlock sob runners
 * paralelos. {@code System.getProperties()} é exatamente o recurso que
 * estamos mutando, então é o monitor natural — não "simplifique" de volta
 * para {@code String.class} ou para um {@code private static Object LOCK}
 * (este último não atravessa classloaders e quebra o bridge).
 *
 * Referência: D-10 — chave gerada em memória, sem chave em disco para testes.
 */
public class JwtTestProfile implements QuarkusTestProfile {

    /** System property keys para bridge entre classloaders (Rule 1). */
    public static final String PRIVATE_KEY_PROP = "mekano.test.jwt.private-key.b64";
    public static final String PUBLIC_KEY_PEM_PROP = "mekano.test.jwt.public-key.pem";

    private static final String PUBLIC_KEY_PEM;

    /**
     * Arquivo PEM de chave pública cacheado em nível de classloader (IN-04 —
     * Code Review 08). Cada invocação de {@link #getConfigOverrides()} antes
     * criava um novo temp file e dependia de {@code deleteOnExit()}; com vários
     * test classes JWT-enabled rodando no mesmo surefire JVM (planejado para v2),
     * isso acumularia arquivos em {@code %TEMP%} até o fim do JVM. Cache em
     * nível de classe garante: um arquivo por classloader, ainda
     * {@code deleteOnExit()} para limpar em shutdown.
     */
    private static final String PUB_PEM_FILE_PATH;

    /**
     * Arquivo PEM da chave privada (PKCS#8) cacheado para sobrescrever
     * {@code smallrye.jwt.sign.key.location} nos testes que exercitam o
     * endpoint POST /auth/login (que assina tokens em runtime).
     */
    private static final String PRIV_PEM_FILE_PATH;

    static {
        try {
            // Quarkus carrega QuarkusTestProfile no launcher classloader e o test class
            // no application classloader; ambos rodam ESTE static block independentemente.
            // Para garantir que pub e priv sejam matched pair, primeiro classloader a
            // chegar gera o KP e publica via System properties (JVM-wide); o segundo
            // detecta e reusa. Sincronização em System.getProperties() (WR-04): é o
            // único objeto JVM-wide alcançável dos dois classloaders SEM colidir
            // com monitores usados por JDK internals (como String.class). Ver
            // Javadoc da classe para o racional completo.
            synchronized (System.getProperties()) {
                String existingPem = System.getProperty(PUBLIC_KEY_PEM_PROP);
                String existingPriv = System.getProperty(PRIVATE_KEY_PROP);
                if (existingPem != null && existingPriv != null) {
                    PUBLIC_KEY_PEM = existingPem;
                } else {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
                    KeyPair kp = generator.generateKeyPair();

                    byte[] pubEncoded = kp.getPublic().getEncoded();
                    // IN-01: charset explícito (US_ASCII) — "\n".getBytes() resolveria ao
                    // default da JVM, anti-pattern flagged por SonarQube/PMD mesmo sendo
                    // benigno para ASCII puro.
                    String pubBase64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                            .encodeToString(pubEncoded);
                    PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\n" + pubBase64 + "\n-----END PUBLIC KEY-----";

                    String privB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
                    System.setProperty(PUBLIC_KEY_PEM_PROP, PUBLIC_KEY_PEM);
                    System.setProperty(PRIVATE_KEY_PROP, privB64);
                }
            }

            // IN-04: materializa a chave pública em UM temp file por classloader.
            // smallrye-jwt resolve `mp.jwt.verify.publickey.location` como URI/file path,
            // então precisamos de um arquivo real (não basta inline). deleteOnExit
            // continua valendo para garantir limpeza em shutdown.
            java.nio.file.Path pem = java.nio.file.Files.createTempFile("jwt-test-pub", ".pem");
            java.nio.file.Files.writeString(pem, PUBLIC_KEY_PEM);
            pem.toFile().deleteOnExit();
            PUB_PEM_FILE_PATH = pem.toAbsolutePath().toString().replace('\\', '/');

            // Materializa também a chave privada em PEM PKCS#8 para
            // smallrye.jwt.sign.key.location — necessário desde que AuthResource
            // emite tokens em runtime via Jwt.sign() (sem PrivateKey explícita).
            String privB64 = System.getProperty(PRIVATE_KEY_PROP);
            byte[] privDer = Base64.getDecoder().decode(privB64);
            String privPemBody = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                    .encodeToString(privDer);
            String privPem = "-----BEGIN PRIVATE KEY-----\n" + privPemBody + "\n-----END PRIVATE KEY-----";
            java.nio.file.Path privPath = java.nio.file.Files.createTempFile("jwt-test-priv", ".pem");
            java.nio.file.Files.writeString(privPath, privPem);
            privPath.toFile().deleteOnExit();
            PRIV_PEM_FILE_PATH = privPath.toAbsolutePath().toString().replace('\\', '/');
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar par Ed25519 para JwtTestProfile", e);
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
            return KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao reconstruir PrivateKey do bridge", e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        // IN-02: apenas a propriedade canônica MicroProfile JWT é necessária.
        // A entrada anterior `smallrye.jwt.verify.key.location` era um nome incorreto
        // (a propriedade SmallRye real seria `smallrye.jwt.verify.publickey.location`),
        // ignorada silenciosamente pelo runtime — removida para não dar a impressão
        // de override duplo intencional.
        //
        // Inline `mp.jwt.verify.publickey` SHOULD take precedence over `.location`,
        // but Quarkus 3.36 emits SRJWT03007 ("Public key is configured but either
        // the secret key or key location are also configured and will be ignored")
        // and in practice falls back to the static publicKey.pem on classpath —
        // signature verify then fails for tokens we sign here. Overriding `.location`
        // com o temp file (cacheado em PUB_PEM_FILE_PATH — IN-04) é inequívoco.
        // smallrye.jwt.sign.algorithm explícito: SmallRye JWT precisa do hint
        // de algoritmo para criar o KeyFactory correto ao ler o PEM Ed25519.
        // Sem ele, a chave PKCS#8 pode não ser reconhecida para signing (SRJWT05028).
        return Map.of(
                "mp.jwt.verify.publickey.location", PUB_PEM_FILE_PATH,
                "smallrye.jwt.sign.key.location", PRIV_PEM_FILE_PATH,
                "smallrye.jwt.sign.algorithm", "EdDSA"
        );
    }
}

