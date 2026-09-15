package pe.edu.upeu.gdmerp.inventario.categoria.dto;

import java.math.BigDecimal;

public record ReporteCategoriaCostoDTO(
    Long categoriaId,
    String categoria,
    Long cantidadProductos,
    BigDecimal montoTotalCosto,
    Double costoPromedio
) {}