package pe.edu.upeu.gdmerp.inventario.producto.dto;

import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto;
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
    CategoriaResponse categoria,
    TipoProducto tipoProducto
) {
    public ProductoResponse(Long id, String sku, String nombre, String unidadMedida, BigDecimal costoPromedio, Integer stockTotal, Integer stockMinimo, AlmacenResponse almacen, CategoriaResponse categoria) {
        this(id, sku, nombre, unidadMedida, costoPromedio, stockTotal, stockMinimo, almacen, categoria, TipoProducto.MATERIA_PRIMA);
    }
}