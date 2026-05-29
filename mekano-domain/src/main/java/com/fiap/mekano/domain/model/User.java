package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.valueobject.Email;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade de domínio User — POJO puro sem anotações JPA.
 *
 * Regras:
 * - Criação APENAS via factory method {@link #create(String, String, String)}.
 * - O builder é privado para forçar o uso do factory method.
 * - Imutável após criação: campos final, sem setters.
 * - O campo {@code passwordHash} é excluído do toString() para evitar
 *   vazamento acidental de hashes em logs.
 *
 * Mapeamento JPA (UserEntity) é responsabilidade do módulo infrastructure (Fase 4).
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class User {

    private final UUID id;
    private final String name;
    private final Email email;

    /**
     * Excluído do toString() para evitar que hashes de senha apareçam em logs.
     * O campo é acessível via {@link #getPasswordHash()} quando necessário.
     */
    @ToString.Exclude
    private final String passwordHash;

    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de um novo usuário.
     *
     * @param name         nome do usuário
     * @param emailValue   string do email (validada pelo Email VO — lança InvalidEmailException se inválido)
     * @param passwordHash hash da senha JÁ calculado pela camada application (BCrypt, Argon2, etc.)
     *                     O domínio armazena apenas o resultado; nunca recebe senha em plaintext.
     * @return nova instância de User com id e createdAt populados automaticamente
     */
    public static User create(String name, String emailValue, String passwordHash) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(new Email(emailValue))
                .passwordHash(passwordHash)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução de um User a partir de dados persistidos.
     *
     * Diferença crítica em relação a {@link #create}: NÃO gera novo UUID nem novo timestamp.
     * Preserva exatamente os valores vindos do banco de dados.
     *
     * Usado exclusivamente por {@code UserEntityMapper.toDomain()} na camada infrastructure.
     *
     * @param id           UUID original do usuário (vindo do banco)
     * @param name         nome do usuário
     * @param emailValue   string do email (revalidada pelo construtor de Email)
     * @param passwordHash hash da senha já armazenado
     * @param createdAt    data/hora original de criação (vindo do banco)
     * @return instância de User com os valores exatos recebidos
     */
    public static User reconstitute(UUID id, String name, String emailValue,
                                     String passwordHash, LocalDateTime createdAt) {
        return User.builder()
                .id(id)
                .name(name)
                .email(new Email(emailValue))
                .passwordHash(passwordHash)
                .createdAt(createdAt)
                .build();
    }
}
