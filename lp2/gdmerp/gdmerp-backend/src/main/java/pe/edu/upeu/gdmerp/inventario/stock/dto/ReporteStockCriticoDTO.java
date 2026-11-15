package pe.edu.upeu.gdmerp.inventario.stock.dto;

import java.math.BigDecimal;

public record ReporteStockCriticoDTO(
        Long productoId,
        String nombreProducto,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        String estado
) {
}
