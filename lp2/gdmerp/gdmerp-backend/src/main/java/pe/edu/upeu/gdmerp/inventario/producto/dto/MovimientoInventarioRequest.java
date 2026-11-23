package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MovimientoInventarioRequest(
        @NotNull(message = "El producto es obligatorio") Long productoId,

        @NotBlank(message = "El tipo de movimiento es obligatorio") String tipoMovimiento, // ENTRADA, SALIDA,
                                                                                           // AJUSTE_POSITIVO,
                                                                                           // AJUSTE_NEGATIVO

        @NotNull(message = "La cantidad es obligatoria") @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor a cero") BigDecimal cantidad,

        BigDecimal costoUnitario, // Opcional: si es null, toma el costoPromedio del producto

        @NotBlank(message = "La referencia o motivo es obligatoria") @Size(max = 120, message = "La referencia no debe superar los 120 caracteres") String referencia) {
}