package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import java.math.BigDecimal;

public record DetalleConsumoDTO(
    Long productoId,
    String nombreProducto,
    BigDecimal cantidadRequerida,
    BigDecimal costoUnitario,
    BigDecimal costoTotal
) {
    public DetalleConsumoDTO(Long productoId, String nombreProducto, Integer cantidadRequerida, BigDecimal costoUnitario, BigDecimal costoTotal) {
        this(
            productoId,
            nombreProducto,
            cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : BigDecimal.ZERO,
            costoUnitario,
            costoTotal
        );
    }
}
