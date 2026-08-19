package com.fiap.mekano.application.service.vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.event.VeiculoCriadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.CreateVeiculoCommand;
import com.fiap.mekano.domain.port.in.UpdateVeiculoCommand;
import com.fiap.mekano.domain.port.in.VeiculoServicePort;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VeiculoService
        implements VeiculoServicePort {

    private final VeiculoRepositoryPort veiculoRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final EventPublisher eventPublisher;

    public VeiculoService(
            VeiculoRepositoryPort veiculoRepository,
            ClienteRepositoryPort clienteRepository,
            EventPublisher eventPublisher) {
        this.veiculoRepository = veiculoRepository;
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Veiculo execute(
            CreateVeiculoCommand command) {

        if (clienteRepository.findById(
                command.clienteUuid()).isEmpty()) {
            throw new AppException(
                    404,
                    "Cliente não encontrado");
        }

        if (veiculoRepository.existsByPlaca(
                command.placa())) {
            throw new AppException(
                    409,
                    "Veículo já cadastrado");
        }

        Veiculo veiculo = Veiculo.create(
                command.clienteUuid(),
                command.placa(),
                command.marca(),
                command.modelo(),
                command.ano());

        Veiculo saved = veiculoRepository.create(veiculo);

        eventPublisher.publish(
                VeiculoCriadoEvent.of(saved));

        return saved;
    }

    @Override
    @Transactional
    public Veiculo update(UUID veiculoId, UpdateVeiculoCommand command) {
        Veiculo existingVehicle = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new AppException(404, "Veículo não encontrado"));

        // A placa é imutável após a criação — qualquer valor enviado é ignorado
        Veiculo updatedVehicle = Veiculo.reconstitute(
                existingVehicle.getId(),
                existingVehicle.getClienteUuid(),
                existingVehicle.getPlaca().getValue(),
                Optional.ofNullable(command.marca()).filter(marca -> !marca.isBlank())
                        .orElse(existingVehicle.getMarca()),
                Optional.ofNullable(command.modelo()).filter(modelo -> !modelo.isBlank())
                        .orElse(existingVehicle.getModelo()),
                Optional.ofNullable(command.ano()).orElse(existingVehicle.getAno()),
                existingVehicle.getCreatedAt());

        return veiculoRepository.update(updatedVehicle);
    }

    @Override
    public Veiculo findById(UUID veiculoId) {
        return veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new AppException(404, "Veículo não encontrado"));
    }

    @Override
    public List<Veiculo> findAll(int page, int size, String sort) {
        return veiculoRepository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return veiculoRepository.countAll();
    }

    @Override
    @Transactional
    public void delete(UUID veiculoId) {
        veiculoRepository.markAsDeleted(veiculoId);
    }

}
