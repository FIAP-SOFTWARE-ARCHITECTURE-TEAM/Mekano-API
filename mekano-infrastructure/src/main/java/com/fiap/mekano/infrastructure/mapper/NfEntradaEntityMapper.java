package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.infrastructure.entity.NfEntradaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "cdi")
public interface NfEntradaEntityMapper {
    NfEntrada toNfEntrada(NfEntradaEntity entity);
    NfEntradaEntity toNfEntradaEntity(NfEntrada nfEntrada);
}
