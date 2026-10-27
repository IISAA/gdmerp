package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionUpdateRequest(
    @NotNull(message = "La cantidad planificada es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad planificada debe ser al menos 0.0001")
    BigDecimal cantidadPlanificada,

    @NotNull(message = "El estado es obligatorio")
    String estado,

    @NotNull(message = "El ID del centro de trabajo es obligatorio")
    Long centroTrabajoId,

    @NotNull(message = "La fecha planificada de inicio es obligatoria")
    LocalDateTime fechaPlanificadaInicio,

    @NotNull(message = "La fecha estimada de fin es obligatoria")
    LocalDateTime fechaEstimadaFin,

    @Valid
    List<DetalleOrdenRequest> detalles
) {
    public OrdenProduccionUpdateRequest(
            Integer cantidadPlanificada,
            String estado,
            Long centroTrabajoId,
            LocalDateTime fechaPlanificadaInicio,
            LocalDateTime fechaEstimadaFin,
            List<DetalleOrdenRequest> detalles) {
        this(
            cantidadPlanificada != null ? BigDecimal.valueOf(cantidadPlanificada) : null,
            estado,
            centroTrabajoId,
            fechaPlanificadaInicio,
            fechaEstimadaFin,
            detalles
        );
    }

    public OrdenProduccionUpdateRequest(
            Double cantidadPlanificada,
            String estado,
            Long centroTrabajoId,
            LocalDateTime fechaPlanificadaInicio,
            LocalDateTime fechaEstimadaFin,
            List<DetalleOrdenRequest> detalles) {
        this(
            cantidadPlanificada != null ? BigDecimal.valueOf(cantidadPlanificada) : null,
            estado,
            centroTrabajoId,
            fechaPlanificadaInicio,
            fechaEstimadaFin,
            detalles
        );
    }
}
