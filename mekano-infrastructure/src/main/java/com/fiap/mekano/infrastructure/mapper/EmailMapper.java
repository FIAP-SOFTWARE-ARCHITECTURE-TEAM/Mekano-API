package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.Email;
import org.mapstruct.Named;

/**
 * Mapeamento auxiliar para conversão entre {@link Email} VO e {@link String}.
 *
 * <p>Usado por {@link UserEntityMapper} via atributo {@code uses}.
 * Registrado como bean CDI (MapStruct detecta {@code @Named} e gera referências).
 *
 * <p>Null-safe: retorna {@code null} se a entrada for {@code null}.
 */
@Named("EmailMapper")
public class EmailMapper {

    /**
     * Converte {@link Email} VO para {@link String}.
     *
     * @param email Value Object de email (pode ser {@code null})
     * @return valor interno do email ou {@code null}
     */
    @Named("emailToString")
    public String emailToString(Email email) {
        return email == null ? null : email.getValue();
    }

    /**
     * Converte {@link String} para {@link Email} VO.
     *
     * @param value string do email (pode ser {@code null})
     * @return novo Email VO ou {@code null}
     * @throws com.fiap.mekano.domain.exception.InvalidEmailException se o formato for inválido
     */
    @Named("stringToEmail")
    public Email stringToEmail(String value) {
        return value == null ? null : new Email(value);
    }
}
