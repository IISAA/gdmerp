package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionRequest(
    @NotNull(message = "El ID del producto terminado es obligatorio")
    Long productoTerminadoId,

    @NotNull(message = "La cantidad planificada es obligatoria")
    @Min(value = 1, message = "La cantidad planificada debe ser al menos 1")
    Integer cantidadPlanificada,

    @NotNull(message = "El estado es obligatorio")
    EstadoOrdenProduccion estado,

    @NotNull(message = "La fecha planificada de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha planificada de inicio no puede ser en el pasado")
    LocalDateTime fechaPlanificadaInicio,

    @NotNull(message = "La fecha estimada de fin es obligatoria")
    @FutureOrPresent(message = "La fecha estimada de fin no puede ser en el pasado")
    LocalDateTime fechaEstimadaFin,

    @NotNull(message = "El ID del centro de trabajo es obligatorio")
    Long centroTrabajoId,

    @NotEmpty(message = "Debe incluir al menos un insumo en el detalle")
    @Valid
    List<DetalleOrdenRequest> detalles
) {}