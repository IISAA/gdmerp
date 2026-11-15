package pe.edu.upeu.gdmerp.inventario.stock.dto;

import java.math.BigDecimal;

public record CostoProductoDTO(
        Long productoId,
        BigDecimal costoPromedio
) {
}
