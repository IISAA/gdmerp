package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

public record DetalleOrdenResponse(
    Long id,
    Long productoId,
    String nombreProducto,
    Integer cantidadRequerida
) {}