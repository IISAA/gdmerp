package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;

public record ReporteStockCriticoDTO(
    Long productoId,
    String nombreProducto,
    BigDecimal stockActual,
    BigDecimal stockMinimo,
    String estado
) {
    public ReporteStockCriticoDTO(Long productoId, String nombreProducto, Long stockActual, BigDecimal stockMinimo, String estado) {
        this(productoId, nombreProducto, stockActual != null ? BigDecimal.valueOf(stockActual) : BigDecimal.ZERO, stockMinimo, estado);
    }

    public ReporteStockCriticoDTO(Long productoId, String nombreProducto, Long stockActual, Integer stockMinimo, String estado) {
        this(productoId, nombreProducto, stockActual != null ? BigDecimal.valueOf(stockActual) : BigDecimal.ZERO, stockMinimo != null ? BigDecimal.valueOf(stockMinimo) : BigDecimal.ZERO, estado);
    }

    public ReporteStockCriticoDTO(Long productoId, String nombreProducto, BigDecimal stockActual, Integer stockMinimo, String estado) {
        this(productoId, nombreProducto, stockActual != null ? stockActual : BigDecimal.ZERO, stockMinimo != null ? BigDecimal.valueOf(stockMinimo) : BigDecimal.ZERO, estado);
    }

    public ReporteStockCriticoDTO(Long productoId, String nombreProducto, Double stockActual, BigDecimal stockMinimo, String estado) {
        this(productoId, nombreProducto, stockActual != null ? BigDecimal.valueOf(stockActual) : BigDecimal.ZERO, stockMinimo, estado);
    }
}
