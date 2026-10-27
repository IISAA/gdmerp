package pe.edu.upeu.gdmerp.inventario.unidadmedida.dto;

import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.CategoriaUnidadMedida;

import java.math.BigDecimal;

public record UnidadMedidaResponse(
    Long id,
    String codigo,
    String codigoSunat,
    String nombre,
    String simbolo,
    CategoriaUnidadMedida categoria,
    BigDecimal factorConversion,
    Boolean permiteDecimales,
    Boolean activo
) {}
