package pe.edu.upeu.gdmerp.inventario.producto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.almacen.mapper.AlmacenMapper;
import pe.edu.upeu.gdmerp.inventario.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CreateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.UpdateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

@Mapper(componentModel = "spring", uses = {AlmacenMapper.class, CategoriaMapper.class})
public interface ProductoMapper {

    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "lotes", ignore = true)
    Producto toEntity(CreateProductoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "lotes", ignore = true)
    void updateEntity(@MappingTarget Producto producto, UpdateProductoRequest request);
}