package com.fiap.mekano.adapter.in.rest;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
 * Fallback (RESEARCH §A3 / Q2): se em algum ambiente o override inline não
 * sobrescrever `.location`, o caminho documentado é escrever o PEM em arquivo
 * temporário e sobrescrever `mp.jwt.verify.publickey.location` aqui — gatilho
 * é falha de boot ao verificar JWT em `UserResourceJwtTest`.
 *
 * Referência: D-10 — chave gerada em memória, sem chave em disco para testes.
 */
public class JwtTestProfile implements QuarkusTestProfile {

    /**
     * Par RSA-2048 carregado uma única vez por execução de teste.
     * Tornado package-default para que `UserResourceJwtTest` (mesmo pacote)
     * acesse `KEY_PAIR.getPrivate()` ao assinar tokens com SmallRyeJwtBuildApi.
     */
    static final KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KEY_PAIR = generator.generateKeyPair();
        } catch (Exception e) {
            // Carregamento estático obrigatório — não há como propagar checked.
            throw new RuntimeException("Falha ao gerar par RSA para JwtTestProfile", e);
        }
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        byte[] encoded = KEY_PAIR.getPublic().getEncoded(); // SubjectPublicKeyInfo
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        String pubPem = "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
        return Map.of("mp.jwt.verify.publickey", pubPem);
    }
}
