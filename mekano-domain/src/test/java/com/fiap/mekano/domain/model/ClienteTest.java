package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.valueobject.Cpf;
import com.fiap.mekano.domain.valueobject.Email;
import com.fiap.mekano.domain.valueobject.Endereco;
import com.fiap.mekano.domain.valueobject.Telefone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Cliente — entidade de dominio")
class ClienteTest {

    private static final String NOME = "Joao Silva";
    private static final String CPF = "52998224725";
    private static final String EMAIL = "joao@email.com";
    private static final String TELEFONE = "11999887766";
    private static final String LOGRADOURO = "Rua das Flores";
    private static final String NUMERO = "123";
    private static final String BAIRRO = "Centro";
    private static final String CIDADE = "Sao Paulo";
    private static final String UF = "SP";
    private static final String CEP = "01001000";

    @Test
    @DisplayName("deve criar Cliente com todos os campos populados")
    void deveCriarClienteComCamposPopulados() {
        Cliente cliente = Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);

        assertNotNull(cliente);
        assertNotNull(cliente.getId());
        assertNotNull(cliente.getCreatedAt());
        assertEquals(NOME, cliente.getNome());
        assertNotNull(cliente.getCpf());
        assertNotNull(cliente.getEmail());
        assertNotNull(cliente.getTelefone());
        assertNotNull(cliente.getEndereco());
        assertEquals(LOGRADOURO, cliente.getEndereco().getLogradouro());
        assertEquals(NUMERO, cliente.getEndereco().getNumero());
        assertEquals(BAIRRO, cliente.getEndereco().getBairro());
        assertEquals(CIDADE, cliente.getEndereco().getCidade());
        assertEquals(UF, cliente.getEndereco().getUf());
        assertEquals(CEP, cliente.getEndereco().getCep());
    }

    @Test
    @DisplayName("campos devem ser dos tipos VO corretos")
    void camposDevemSerVOCorretos() {
        Cliente cliente = Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);

        assertInstanceOf(Cpf.class, cliente.getCpf());
        assertEquals(CPF, cliente.getCpf().getValue());
        assertInstanceOf(Email.class, cliente.getEmail());
        assertEquals(EMAIL, cliente.getEmail().getValue());
        assertInstanceOf(Telefone.class, cliente.getTelefone());
        assertEquals(TELEFONE, cliente.getTelefone().getValue());
        assertInstanceOf(Endereco.class, cliente.getEndereco());
    }

    @Test
    @DisplayName("deve criar Cliente sem telefone (opcional)")
    void deveCriarClienteSemTelefone() {
        Cliente cliente = Cliente.create(NOME, CPF, EMAIL, null,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);

        assertNotNull(cliente);
        assertNull(cliente.getTelefone());
    }

    @Test
    @DisplayName("deve criar Cliente com telefone em branco como null")
    void deveCriarClienteComTelefoneEmBrancoComoNull() {
        Cliente cliente = Cliente.create(NOME, CPF, EMAIL, "   ",
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);

        assertNull(cliente.getTelefone());
    }

    @Test
    @DisplayName("endereco deve validar e normalizar UF para uppercase")
    void enderecoDeveNormalizarUf() {
        Cliente cliente = Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, "sp", CEP);
        assertEquals("SP", cliente.getEndereco().getUf());
    }

    @Test
    @DisplayName("deve lancar AppException para endereco com logradouro null")
    void deveLancarExcecaoParaEnderecoInvalido() {
        assertThrows(AppException.class,
                () -> Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                        null, NUMERO, BAIRRO, CIDADE, UF, CEP));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para CPF invalido")
    void deveLancarExcecaoParaCpfInvalido() {
        assertThrows(AppException.class,
                () -> Cliente.create(NOME, "00000000000", EMAIL, TELEFONE,
                        LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para email invalido")
    void deveLancarExcecaoParaEmailInvalido() {
        assertThrows(AppException.class,
                () -> Cliente.create(NOME, CPF, "email-invalido", TELEFONE,
                        LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP));
    }

    @Test
    @DisplayName("deve lancar AppException(400) para telefone invalido")
    void deveLancarExcecaoParaTelefoneInvalido() {
        assertThrows(AppException.class,
                () -> Cliente.create(NOME, CPF, EMAIL, "123",
                        LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP));
    }

    @Test
    @DisplayName("duas chamadas a create devem gerar IDs distintos")
    void duasChamadasDevemGerarIdsDistintos() {
        Cliente c1 = Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);
        Cliente c2 = Cliente.create(NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);

        assertNotEquals(c1.getId(), c2.getId());
    }

    @Test
    @DisplayName("reconstitute deve preservar UUID e createdAt exatos")
    void reconstituteDevePreservarUuidECreatedAt() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 15, 10, 30, 0);

        Cliente cliente = Cliente.reconstitute(id, NOME, CPF, EMAIL, TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP, createdAt);

        assertEquals(id, cliente.getId());
        assertEquals(createdAt, cliente.getCreatedAt());
    }

    @Test
    @DisplayName("reconstitute deve revalidar VOs")
    void reconstituteDeveRevalidarVOs() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        assertThrows(AppException.class,
                () -> Cliente.reconstitute(id, NOME, "cpf-invalido", EMAIL, TELEFONE,
                        LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP, createdAt));
    }

    @Test
    @DisplayName("email deve ser normalizado para lowercase pelo VO")
    void emailDeveSerNormalizado() {
        Cliente cliente = Cliente.create(NOME, CPF, "JOAO@EMAIL.COM", TELEFONE,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP);
        assertEquals("joao@email.com", cliente.getEmail().getValue());
    }

    @Test
    @DisplayName("reconstitute deve funcionar sem telefone")
    void reconstituteDeveFuncionarSemTelefone() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        Cliente cliente = Cliente.reconstitute(id, NOME, CPF, EMAIL, null,
                LOGRADOURO, NUMERO, BAIRRO, CIDADE, UF, CEP, createdAt);

        assertEquals(id, cliente.getId());
        assertNull(cliente.getTelefone());
        assertNotNull(cliente.getEndereco());
    }
}
