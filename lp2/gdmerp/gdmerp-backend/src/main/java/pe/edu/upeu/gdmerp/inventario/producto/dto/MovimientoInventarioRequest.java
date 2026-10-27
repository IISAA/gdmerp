package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MovimientoInventarioRequest(
    @NotNull(message = "El ID del producto es obligatorio")
    Long productoId,

    @NotBlank(message = "El tipo de movimiento es obligatorio")
    String tipoMovimiento,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a 0")
    BigDecimal cantidad,

    BigDecimal costoUnitario,

    String referencia
) {
    public MovimientoInventarioRequest(Long productoId, String tipoMovimiento, Integer cantidad, String referencia) {
        this(productoId, tipoMovimiento, cantidad != null ? BigDecimal.valueOf(cantidad) : null, null, referencia);
    }

    public MovimientoInventarioRequest(Long productoId, String tipoMovimiento, Double cantidad, String referencia) {
        this(productoId, tipoMovimiento, cantidad != null ? BigDecimal.valueOf(cantidad) : null, null, referencia);
    }
}
