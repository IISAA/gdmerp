package pe.edu.upeu.gdmerp.produccion.listamateriales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ListaMaterialesRequest(
    @NotBlank(message = "El código es obligatorio")
    String codigo,

    @NotNull(message = "El ID del producto terminado es obligatorio")
    Long productoTerminadoId,

    @NotNull(message = "La cantidad base es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad base debe ser mayor a 0")
    BigDecimal cantidadBase,

    @NotEmpty(message = "La receta debe contener al menos un insumo")
    List<@Valid DetalleListaMaterialesRequest> detalles
) {}
