package pe.edu.upeu.gdmerp.inventario.producto.dto;

public record ReporteStockCriticoDTO(
    Long productoId,
    String nombreProducto,
    Long stockActual,
    Integer stockMinimo,
    String estado
) {}