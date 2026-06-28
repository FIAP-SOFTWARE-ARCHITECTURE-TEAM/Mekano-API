package com.fiap.mekano.domain.port.in;


public interface AuthServicePort {

    TokenPair login(LoginCommand command);

    TokenPair refresh(String refreshToken);

    void logout(String refreshToken);
}
