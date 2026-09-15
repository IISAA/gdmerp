package pe.edu.upeu.gdmerp.inventario.producto.dto;

public record ReporteStockProductoDTO(
    Long productoId,
    String nombreProducto,
    String categoria,
    Long stockTotal
) {}