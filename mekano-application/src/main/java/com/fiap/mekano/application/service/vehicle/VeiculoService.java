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
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.valueobject.PlacaVeiculo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

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

        // 1. Tratar a placa do comando de forma segura e normalizada
        Optional<String> newPlacaNormalized = Optional.ofNullable(command.placa())
                .filter(placa -> !placa.isBlank())
                .map(PlacaVeiculo::new) // Assumindo que PlacaVeiculo tem um construtor que aceita String
                .map(PlacaVeiculo::getValue);

        // 2. Validar se a nova placa (se presente e diferente) já existe
        newPlacaNormalized.ifPresent(placa -> {
            if (!placa.equalsIgnoreCase(existingVehicle.getPlaca().getValue())
                    && veiculoRepository.existsByPlaca(placa)) {
                throw new AppException(409, "Veículo já cadastrado");
            }
        });

        // 3. Reconstituir o veículo com os valores atualizados ou existentes
        Veiculo updatedVehicle = Veiculo.reconstitute(
                existingVehicle.getId(),
                existingVehicle.getClienteUuid(),
                newPlacaNormalized.orElseGet(() -> existingVehicle.getPlaca().getValue()),
                Optional.ofNullable(command.marca()).filter(marca -> !marca.isBlank())
                        .orElse(existingVehicle.getMarca()),
                Optional.ofNullable(command.modelo()).filter(modelo -> !modelo.isBlank())
                        .orElse(existingVehicle.getModelo()),
                Optional.ofNullable(command.ano()).orElse(existingVehicle.getAno()),
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
