package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.valueobject.Email;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User — entidade de domínio")
class UserTest {

    private static final String NOME_VALIDO = "João Silva";
    private static final String EMAIL_VALIDO = "joao@fiap.br";
    private static final String HASH_VALIDO = "bcrypt:$2a$12$hashdummy";

    @Test
    @DisplayName("deve criar User com todos os campos populados")
    void deveCriarUserComCamposPopulados() {
        User user = User.create(NOME_VALIDO, EMAIL_VALIDO, HASH_VALIDO);

        assertNotNull(user);
        assertNotNull(user.getId(), "id deve ser não nulo");
        assertNotNull(user.getCreatedAt(), "createdAt deve ser não nulo");
        assertEquals(NOME_VALIDO, user.getName());
        assertNotNull(user.getEmail(), "campo email (VO) deve ser não nulo");
        assertNotNull(user.getPasswordHash(), "passwordHash deve ser não nulo");
    }

    @Test
    @DisplayName("campo email deve ser do tipo Email VO")
    void campoEmailDeveSerVO() {
        User user = User.create(NOME_VALIDO, EMAIL_VALIDO, HASH_VALIDO);
        assertInstanceOf(Email.class, user.getEmail());
        assertEquals(EMAIL_VALIDO, user.getEmail().getValue());
    }

    @Test
    @DisplayName("deve lançar AppException(400) para email inválido")
    void deveLancarExcecaoParaEmailInvalido() {
        assertThrows(AppException.class,
                () -> User.create(NOME_VALIDO, "email-invalido", HASH_VALIDO));
    }

    @Test
    @DisplayName("deve lançar AppException(400) para email null")
    void deveLancarExcecaoParaEmailNull() {
        assertThrows(AppException.class,
                () -> User.create(NOME_VALIDO, null, HASH_VALIDO));
    }

    @Test
    @DisplayName("toString não deve conter o passwordHash")
    void toStringNaoDeveConterPasswordHash() {
        String hashSecreto = "mySuperSecretHash$$$";
        User user = User.create(NOME_VALIDO, EMAIL_VALIDO, hashSecreto);

        String representacao = user.toString();
        assertFalse(representacao.contains(hashSecreto),
                "toString() não deve expor o passwordHash — risco de segurança em logs");
    }

    @Test
    @DisplayName("duas chamadas a create devem gerar IDs distintos")
    void duasChamadasDevemGerarIdDistintos() {
        User user1 = User.create(NOME_VALIDO, EMAIL_VALIDO, HASH_VALIDO);
        User user2 = User.create(NOME_VALIDO, EMAIL_VALIDO, HASH_VALIDO);

        assertNotEquals(user1.getId(), user2.getId(),
                "cada chamada a create() deve gerar um UUID único");
    }

    @Test
    @DisplayName("email deve ser normalizado para lowercase pelo VO")
    void emailDeveSerNormalizado() {
        User user = User.create(NOME_VALIDO, "JOAO@FIAP.BR", HASH_VALIDO);
        assertEquals("joao@fiap.br", user.getEmail().getValue());
    }
}
