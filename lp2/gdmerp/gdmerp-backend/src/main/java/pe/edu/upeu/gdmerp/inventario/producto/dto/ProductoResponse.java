package pe.edu.upeu.gdmerp.inventario.producto.dto;

import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;
import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    String sku,
    String nombre,
    String unidadMedida,
    BigDecimal costoPromedio,
    Integer stockTotal,
    Integer stockMinimo,
    AlmacenResponse almacen,
    CategoriaResponse categoria
) {}