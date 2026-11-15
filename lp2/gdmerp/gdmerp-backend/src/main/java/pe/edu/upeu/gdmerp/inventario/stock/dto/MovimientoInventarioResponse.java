package pe.edu.upeu.gdmerp.inventario.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Long id,
        Long productoId,
        String sku,
        String nombreProducto,
        Long almacenId,
        String tipoMovimiento,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockResultante,
        BigDecimal costoUnitario,
        LocalDateTime fechaMovimiento,
        String referencia
) {
}
