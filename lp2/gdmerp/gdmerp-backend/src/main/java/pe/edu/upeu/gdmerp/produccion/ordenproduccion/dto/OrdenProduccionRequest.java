package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionRequest(
    @NotNull(message = "El ID del producto terminado es obligatorio")
    @JsonProperty("productoTerminadoId")
    @JsonAlias({"productoId", "producto_terminado_id", "producto_id"})
    Long productoTerminadoId,

    @NotNull(message = "La cantidad planificada es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad planificada debe ser al menos 0.0001")
    @JsonProperty("cantidadPlanificada")
    @JsonAlias({"cantidad", "cantidad_planificada"})
    BigDecimal cantidadPlanificada,

    @NotNull(message = "El estado es obligatorio")
    EstadoOrdenProduccion estado,

    @NotNull(message = "La fecha planificada de inicio es obligatoria")
    LocalDateTime fechaPlanificadaInicio,

    @NotNull(message = "La fecha estimada de fin es obligatoria")
    LocalDateTime fechaEstimadaFin,

    @NotNull(message = "El ID del centro de trabajo es obligatorio")
    @JsonProperty("centroTrabajoId")
    @JsonAlias({"centro_trabajo_id"})
    Long centroTrabajoId,

    @NotEmpty(message = "Debe incluir al menos un insumo en el detalle")
    @Valid
    List<DetalleOrdenRequest> detalles
) {
    public Long productoId() {
        return productoTerminadoId;
    }

    public BigDecimal cantidad() {
        return cantidadPlanificada;
    }

    public OrdenProduccionRequest(
        Long productoTerminadoId,
        Integer cantidadPlanificada,
        EstadoOrdenProduccion estado,
        LocalDateTime fechaPlanificadaInicio,
        LocalDateTime fechaEstimadaFin,
        Long centroTrabajoId,
        List<DetalleOrdenRequest> detalles
    ) {
        this(
            productoTerminadoId,
            cantidadPlanificada != null ? BigDecimal.valueOf(cantidadPlanificada) : null,
            estado,
            fechaPlanificadaInicio,
            fechaEstimadaFin,
            centroTrabajoId,
            detalles
        );
    }
}
