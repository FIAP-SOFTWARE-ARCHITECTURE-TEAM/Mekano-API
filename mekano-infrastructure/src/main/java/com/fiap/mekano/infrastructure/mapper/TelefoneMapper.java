package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.Telefone;
import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Named;

@ApplicationScoped
public class TelefoneMapper {

    @Named("telefoneToString")
    public String telefoneToString(Telefone telefone) {
        return telefone == null ? null : telefone.getValue();
    }

    @Named("stringToTelefone")
    public Telefone stringToTelefone(String value) {
        return value == null ? null : new Telefone(value);
    }
}
