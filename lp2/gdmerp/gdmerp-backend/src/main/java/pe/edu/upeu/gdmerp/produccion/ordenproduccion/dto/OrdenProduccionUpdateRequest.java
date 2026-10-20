package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionUpdateRequest(
    @NotNull(message = "La cantidad planificada es obligatoria")
    @Min(value = 1, message = "La cantidad planificada debe ser al menos 1")
    Integer cantidadPlanificada,

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
    // Constructor de compatibilidad en caso de invocarse con Double en cantidadPlanificada
    public OrdenProduccionUpdateRequest(
            Double cantidadPlanificada,
            String estado,
            Long centroTrabajoId,
            LocalDateTime fechaPlanificadaInicio,
            LocalDateTime fechaEstimadaFin,
            List<DetalleOrdenRequest> detalles) {
        this(
            cantidadPlanificada != null ? cantidadPlanificada.intValue() : null,
            estado,
            centroTrabajoId,
            fechaPlanificadaInicio,
            fechaEstimadaFin,
            detalles
        );
    }
}
