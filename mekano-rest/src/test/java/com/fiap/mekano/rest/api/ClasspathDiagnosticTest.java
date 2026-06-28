package com.fiap.mekano.rest.api;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import com.fiap.mekano.domain.port.in.AuthServicePort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ClasspathDiagnosticTest {

    @Inject
    AuthServicePort authService;

    @Test
    public void testAuthServiceInjected() {
        assertNotNull(authService, "AuthServicePort should be injected");
    }

    @Test
    public void testAuthServiceClassLoadable() throws Exception {
        Class<?> clazz = Class.forName("com.fiap.mekano.application.service.auth.AuthService");
        assertNotNull(clazz);
        assertTrue(clazz.isAnnotationPresent(jakarta.enterprise.context.ApplicationScoped.class), "AuthService should be ApplicationScoped");
    }
}
