package pe.edu.upeu.gdmerp.inventario.stock.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.stock.dto.AlmacenResumen;
import pe.edu.upeu.gdmerp.inventario.stock.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.stock.entity.StockInventario;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InventarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "version", ignore = true)
    StockInventario toEntity(StockInventarioRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productoId", ignore = true)
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(@MappingTarget StockInventario stock, StockInventarioRequest request);

    @Mapping(target = "id", source = "stock.id")
    @Mapping(target = "productoId", source = "stock.productoId")
    @Mapping(target = "sku", source = "catalogo.sku")
    @Mapping(target = "nombreProducto", source = "catalogo.nombre")
    @Mapping(target = "unidadMedida", source = "catalogo.unidadMedida")
    @Mapping(target = "tipoProducto", source = "catalogo.tipoProducto")
    @Mapping(target = "precioVenta", source = "catalogo.precio")
    @Mapping(target = "almacen", source = "stock.almacen")
    @Mapping(target = "categoriaId", source = "stock.categoriaId")
    @Mapping(target = "stockActual", source = "stock.stockActual")
    @Mapping(target = "stockMinimo", source = "stock.stockMinimo")
    @Mapping(target = "stockMaximo", source = "stock.stockMaximo")
    @Mapping(target = "costoPromedio", source = "stock.costoPromedio")
    StockInventarioResponse toResponse(StockInventario stock, ProductoResponse catalogo);

    @Mapping(target = "id", source = "stock.id")
    @Mapping(target = "productoId", source = "stock.productoId")
    @Mapping(target = "sku", source = "catalogo.sku")
    @Mapping(target = "nombreProducto", source = "catalogo.nombre")
    @Mapping(target = "unidadMedida", source = "catalogo.unidadMedida")
    @Mapping(target = "tipoProducto", source = "catalogo.tipoProducto")
    @Mapping(target = "precioVenta", source = "catalogo.precio")
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "categoriaId", source = "stock.categoriaId")
    @Mapping(target = "stockActual", source = "stock.stockActual")
    @Mapping(target = "stockMinimo", source = "stock.stockMinimo")
    @Mapping(target = "stockMaximo", source = "stock.stockMaximo")
    @Mapping(target = "costoPromedio", source = "stock.costoPromedio")
    StockInventarioResponse toResponseSinAlmacen(StockInventario stock, ProductoResponse catalogo);

    AlmacenResumen toAlmacenResumen(Almacen almacen);

    @Mapping(target = "id", source = "movimiento.id")
    @Mapping(target = "productoId", source = "movimiento.productoId")
    @Mapping(target = "sku", source = "catalogo.sku")
    @Mapping(target = "nombreProducto", source = "catalogo.nombre")
    @Mapping(target = "almacenId", source = "movimiento.almacenId")
    @Mapping(target = "tipoMovimiento", source = "movimiento.tipoMovimiento")
    @Mapping(target = "cantidad", source = "movimiento.cantidad")
    @Mapping(target = "stockAnterior", source = "movimiento.stockAnterior")
    @Mapping(target = "stockResultante", source = "movimiento.stockResultante")
    @Mapping(target = "costoUnitario", source = "movimiento.costoUnitario")
    @Mapping(target = "fechaMovimiento", source = "movimiento.fechaMovimiento")
    @Mapping(target = "referencia", source = "movimiento.referencia")
    MovimientoInventarioResponse toMovimientoResponse(MovimientoInventario movimiento, ProductoResponse catalogo);
}
