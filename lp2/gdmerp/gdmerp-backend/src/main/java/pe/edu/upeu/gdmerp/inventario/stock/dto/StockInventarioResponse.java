package pe.edu.upeu.gdmerp.inventario.stock.dto;

import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;

import java.math.BigDecimal;

public record StockInventarioResponse(
        Long id,
        Long productoId,
        String sku,
        String nombreProducto,
        String unidadMedida,
        TipoProducto tipoProducto,
        BigDecimal precioVenta,
        AlmacenResumen almacen,
        Long categoriaId,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        BigDecimal costoPromedio
) {
}
