package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleOrdenRequest(
    @NotNull(message = "El ID del producto es obligatorio") Long productoId,
    @NotNull(message = "La cantidad es obligatoria") @Min(value = 1) Integer cantidadRequerida
) {}