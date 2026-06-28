package com.fiap.mekano.domain.port.out;

public interface PasswordHasherPort {

    boolean matches(String rawPassword, String passwordHash);

    String hash(String rawPassword);
}
