package com.fiap.mekano.application.service.vehicle;

import com.fiap.mekano.domain.event.VeiculoCriadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.in.CreateVeiculoCommand;
import com.fiap.mekano.domain.port.in.UpdateVeiculoCommand;
import com.fiap.mekano.domain.port.in.VeiculoServicePort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.valueobject.PlacaVeiculo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VeiculoService
        implements VeiculoServicePort {

    private final VeiculoRepositoryPort veiculoRepository;
    private final UserRepositoryPort clienteRepository;
    private final EventPublisher eventPublisher;

    public VeiculoService(
            VeiculoRepositoryPort veiculoRepository,
            UserRepositoryPort clienteRepository,
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

        Veiculo saved = veiculoRepository.save(veiculo);

        eventPublisher.publish(
                VeiculoCriadoEvent.of(saved));

        return saved;
    }

    @Override
    @Transactional
    public Veiculo update(UUID veiculoId, UpdateVeiculoCommand command) {
        Veiculo existingVehicle = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new AppException(404, "Veículo não encontrado"));

        String placaNormalizada = new PlacaVeiculo(
                command.placa())
                .getValue();

        if (command.placa() != null && !command.placa().isBlank()
                && !existingVehicle.getPlaca().getValue().equalsIgnoreCase(placaNormalizada)
                && veiculoRepository.existsByPlaca(placaNormalizada)) {
            throw new AppException(409, "Veículo já cadastrado");
        }

        Veiculo updatedVehicle = Veiculo.reconstitute(
                existingVehicle.getId(),
                existingVehicle.getClienteUuid(),
                command.placa() != null && !command.placa().isBlank() ? placaNormalizada
                        : existingVehicle.getPlaca().getValue(),
                command.marca() != null && !command.marca().isBlank() ? command.marca() : existingVehicle.getMarca(),
                command.modelo() != null && !command.modelo().isBlank() ? command.modelo()
                        : existingVehicle.getModelo(),
                command.ano() != null ? command.ano() : existingVehicle.getAno(),
                existingVehicle.getCreatedAt());

        return veiculoRepository.save(updatedVehicle);
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
