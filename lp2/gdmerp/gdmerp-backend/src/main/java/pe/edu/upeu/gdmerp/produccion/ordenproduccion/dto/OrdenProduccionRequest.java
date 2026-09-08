package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrdenProduccionRequest(
    @NotNull(message = "El ID del producto terminado es obligatorio") Long productoTerminadoId,
    @NotBlank(message = "El nombre del producto es obligatorio") String producto,
    @NotNull(message = "La cantidad planificada es obligatoria") @Min(value = 1) Integer cantidadPlanificada,
    @NotBlank(message = "El estado es obligatorio") String estado,
    @NotNull(message = "El ID del centro de trabajo es obligatorio") Long centroTrabajoId,
    @NotEmpty(message = "Debe incluir al menos un insumo en el detalle") @Valid List<DetalleOrdenRequest> detalles
) {}