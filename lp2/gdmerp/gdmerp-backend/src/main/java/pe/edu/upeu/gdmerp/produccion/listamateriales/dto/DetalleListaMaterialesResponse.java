package pe.edu.upeu.gdmerp.produccion.listamateriales.dto;

import java.math.BigDecimal;

public record DetalleListaMaterialesResponse(
    Long id,
    Long insumoId,
    String nombreInsumo,
    String skuInsumo,
    String unidadMedidaInsumo,
    BigDecimal cantidadRequerida,
    BigDecimal costoPromedioInsumo
) {}
