package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;

public record CostoProductoDTO(
    Long productoId,
    BigDecimal costoPromedio
) {}