package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.valueobject.Cpf;
import jakarta.enterprise.context.ApplicationScoped;
import org.mapstruct.Named;

@ApplicationScoped
public class CpfMapper {

    @Named("cpfToString")
    public String cpfToString(Cpf cpf) {
        return cpf == null ? null : cpf.getValue();
    }

    @Named("stringToCpf")
    public Cpf stringToCpf(String value) {
        return value == null ? null : new Cpf(value);
    }
}
