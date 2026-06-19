package com.fiap.mekano.rest.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@ApplicationScoped
public class SigningKeyProducer {

    @Inject
    @ConfigProperty(name = "smallrye.jwt.sign.key.location")
    String keyLocation;

    @Produces
    @ApplicationScoped
    public PrivateKey privateKey() {
        try {
            String pem = Files.readString(Path.of(keyLocation));

            String base64 = pem
                    .replaceAll("-----BEGIN [A-Z ]+-----", "")
                    .replaceAll("-----END [A-Z ]+-----", "")
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("Ed25519").generatePrivate(spec);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load signing key from " + keyLocation, e);
        }
    }
}
