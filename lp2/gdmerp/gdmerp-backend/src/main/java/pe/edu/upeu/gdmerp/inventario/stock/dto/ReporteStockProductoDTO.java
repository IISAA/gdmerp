package pe.edu.upeu.gdmerp.inventario.stock.dto;

import java.math.BigDecimal;

public record ReporteStockProductoDTO(
        Long productoId,
        String nombreProducto,
        String categoria,
        BigDecimal stockTotal
) {
}
