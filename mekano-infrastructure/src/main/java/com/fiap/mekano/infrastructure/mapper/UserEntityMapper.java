package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Mapper MapStruct entre {@link UserEntity} (JPA) e {@link User} (domínio).
 *
 * <p>Reabilitado na Phase 9 (09-05) após isolamento do bug
 * quarkus-maven-plugin generate-code. Estratégia de null: {@code RETURN_DEFAULT}.
 *
 * <p>O método {@link #toDomain(UserEntity)} usa implementação {@code default}
 * porque {@link User} tem {@code @Builder(access = PRIVATE)} — MapStruct não
 * consegue instanciar {@code User} diretamente. A conversão delega a
 * {@link User#reconstitute}.
 *
 * <p>O campo email é convertido entre {@code String} (JPA) e {@code Email} VO
 * (domínio) via {@link EmailMapper} (declarado em {@code uses}).
 */
@Mapper(componentModel = "cdi", uses = {EmailMapper.class})
public interface UserEntityMapper {

    /**
     * Converte entidade de domínio para entidade JPA.
     *
     * @param user entidade de domínio (pode ser {@code null})
     * @return entidade JPA pronta para persistência, ou {@code null}
     */
    @Mapping(target = "email", source = "email", qualifiedByName = "emailToString")
    UserEntity toEntity(User user);

    /**
     * Reconstrói entidade de domínio a partir da entidade JPA.
     *
     * <p>Implementação default porque {@code User.@Builder(access = PRIVATE)}
     * impede o MapStruct de gerar o construtor/factory automaticamente.
     * Delega para {@link User#reconstitute}.
     *
     * @param entity entidade JPA vinda do banco (pode ser {@code null})
     * @return instância de User com valores exatos do banco, ou {@code null}
     */
    default User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}
