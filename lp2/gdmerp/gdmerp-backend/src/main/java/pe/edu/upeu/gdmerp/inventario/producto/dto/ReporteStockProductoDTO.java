package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;

public record ReporteStockProductoDTO(
    Long productoId,
    String nombreProducto,
    String categoria,
    BigDecimal stockTotal
) {
    public ReporteStockProductoDTO(Long productoId, String nombreProducto, String categoria, Long stockTotal) {
        this(productoId, nombreProducto, categoria, stockTotal != null ? BigDecimal.valueOf(stockTotal) : BigDecimal.ZERO);
    }

    public ReporteStockProductoDTO(Long productoId, String nombreProducto, String categoria, Double stockTotal) {
        this(productoId, nombreProducto, categoria, stockTotal != null ? BigDecimal.valueOf(stockTotal) : BigDecimal.ZERO);
    }

    public ReporteStockProductoDTO(Long productoId, String nombreProducto, String categoria, Integer stockTotal) {
        this(productoId, nombreProducto, categoria, stockTotal != null ? BigDecimal.valueOf(stockTotal) : BigDecimal.ZERO);
    }
}
