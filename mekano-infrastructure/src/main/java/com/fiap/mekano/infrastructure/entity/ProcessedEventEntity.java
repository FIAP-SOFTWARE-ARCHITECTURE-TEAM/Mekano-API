package com.fiap.mekano.infrastructure.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_processed_events_uuid", columnList = "uuid_agregado"),
    @Index(name = "idx_processed_events_tipo", columnList = "tipo_evento"),
    @Index(name = "idx_processed_events_evento_id", columnList = "evento_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true, length = 255)
    private String eventoId;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(name = "origem_agregado", nullable = false, length = 50)
    private String origemAgregado;

    @Column(name = "uuid_agregado", nullable = false)
    private UUID uuidAgregado;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processado_em", nullable = false, updatable = false)
    private LocalDateTime processadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        if (processadoEm == null) {
            processadoEm = LocalDateTime.now();
        }
    }
}
