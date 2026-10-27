package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
    Long id,
    Long productoId,
    String nombreProducto,
    String skuProducto,
    String tipoMovimiento,
    BigDecimal cantidad,
    BigDecimal stockAnterior,
    BigDecimal stockResultante,
    BigDecimal costoUnitario,
    LocalDateTime fechaMovimiento,
    String referencia
) {}
