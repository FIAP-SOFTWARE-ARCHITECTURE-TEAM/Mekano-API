package com.fiap.mekano.domain.port.out;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepositoryPort {

    Optional<RefreshTokenData> findByTokenHash(String tokenHash);

    RefreshTokenData save(RefreshTokenData data);

    void deleteByUser(UUID userUuid);
}
