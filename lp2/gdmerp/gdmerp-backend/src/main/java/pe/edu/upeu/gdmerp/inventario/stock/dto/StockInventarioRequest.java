package pe.edu.upeu.gdmerp.inventario.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record StockInventarioRequest(
        @NotNull(message = "El identificador del producto en catálogo es obligatorio")
        Long productoId,

        Long almacenId,

        Long categoriaId,

        @PositiveOrZero(message = "El stock actual no puede ser negativo")
        BigDecimal stockActual,

        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        BigDecimal stockMinimo,

        @PositiveOrZero(message = "El stock máximo no puede ser negativo")
        BigDecimal stockMaximo,

        @PositiveOrZero(message = "El costo promedio no puede ser negativo")
        BigDecimal costoPromedio
) {
}
