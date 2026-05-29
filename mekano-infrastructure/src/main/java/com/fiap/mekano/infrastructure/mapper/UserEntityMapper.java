package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.valueobject.Email;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper CDI para conversão entre {@link UserEntity} (JPA) e {@link User} (domain).
 *
 * <p>Estratégia de conversão:
 * <ul>
 *   <li>{@code toEntity(User)} — gerado automaticamente pelo MapStruct (UserEntity tem @NoArgsConstructor + @Setter)</li>
 *   <li>{@code toDomain(UserEntity)} — implementado manualmente como default method porque
 *       {@code User} usa {@code @Builder(access = PRIVATE)} — MapStruct não consegue usar o builder diretamente (D-04)</li>
 *   <li>{@code emailToString(Email)} — conversão Email VO → String; detectada automaticamente por MapStruct ao gerar toEntity()</li>
 *   <li>{@code emailFromString(String)} — conversão inversa; não usada diretamente (toDomain é manual), mantida por completude</li>
 * </ul>
 *
 * <p>componentModel = "cdi": bean CDI gerenciado pelo Quarkus Arc (STATE.md Decision 4).
 * NUNCA usar "spring" neste projeto.
 */
@Mapper(componentModel = "cdi")
public interface UserEntityMapper {

    /**
     * Converte entidade de domínio para entidade JPA.
     * Gerado pelo MapStruct: usa @NoArgsConstructor + setters de UserEntity.
     * MapStruct detecta emailToString() automaticamente para mapear User.email (Email) → UserEntity.email (String).
     *
     * @param user entidade de domínio
     * @return entidade JPA pronta para persistência
     */
    UserEntity toEntity(User user);

    /**
     * Reconstrói entidade de domínio a partir da entidade JPA.
     *
     * Implementado como default method porque User.@Builder(access = PRIVATE) impede
     * que MapStruct use o builder externamente. User.reconstitute() tem acesso interno
     * ao builder privado e preserva os valores originais de id e createdAt (D-03, D-04).
     *
     * @param entity entidade JPA vinda do banco
     * @return instância de User com valores exatos do banco
     */
    default User toDomain(UserEntity entity) {
        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }

    /**
     * Conversão auxiliar: Email VO → String.
     * Detectada automaticamente pelo MapStruct ao gerar toEntity().
     *
     * @param email Value Object Email
     * @return string do email normalizada (lowercase)
     */
    default String emailToString(Email email) {
        return email.getValue();
    }

    /**
     * Conversão auxiliar: String → Email VO.
     * Mantida para completude; não usada diretamente pois toDomain() é implementado manualmente.
     *
     * @param value string do email
     * @return Email VO validado
     */
    default Email emailFromString(String value) {
        return new Email(value);
    }
}
