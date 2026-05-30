package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.valueobject.Email;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper CDI para conversão entre {@link UserEntity} (JPA) e {@link User} (domínio).
 *
 * <p><b>Histórico:</b> originalmente uma interface MapStruct ({@code @Mapper(componentModel = "cdi")}).
 * Substituído por bean concreto {@code @ApplicationScoped} em razão do mesmo bug que motivou
 * o swap do {@code UserDtoMapper} no módulo adapter durante a Fase 8 (08-05): o mojo
 * {@code generate-code} do Quarkus 3.36 corrompe silenciosamente o bytecode gerado pelo
 * processador MapStruct quando o build agregado executa múltiplos {@code @QuarkusTest} em
 * sequência, causando {@code UnsatisfiedResolutionException} no scanner CDI ao subir o
 * {@code UserRepositoryImplTest}. Per-class o teste passava; agregado falhava (UAT-4).
 * Implementação manual elimina a dependência do annotation processor.
 *
 * <p>API pública preservada: {@link #toEntity(User)} e {@link #toDomain(UserEntity)}.
 * Métodos auxiliares de conversão {@code Email <-> String} estão inline (não eram chamados
 * externamente).
 */
@ApplicationScoped
public class UserEntityMapper {

    /**
     * Converte entidade de domínio para entidade JPA.
     *
     * @param user entidade de domínio
     * @return entidade JPA pronta para persistência
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail() != null ? user.getEmail().getValue() : null);
        entity.setPasswordHash(user.getPasswordHash());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    /**
     * Reconstrói entidade de domínio a partir da entidade JPA.
     *
     * <p>Usa {@link User#reconstitute} porque {@code User.@Builder(access = PRIVATE)} impede
     * uso externo do builder (D-03, D-04).
     *
     * @param entity entidade JPA vinda do banco
     * @return instância de User com valores exatos do banco
     */
    public User toDomain(UserEntity entity) {
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
