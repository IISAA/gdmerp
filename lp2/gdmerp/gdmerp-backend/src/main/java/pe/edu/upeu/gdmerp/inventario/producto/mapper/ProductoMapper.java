package pe.edu.upeu.gdmerp.inventario.producto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.almacen.mapper.AlmacenMapper;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

@Mapper(componentModel = "spring", uses = {AlmacenMapper.class})
public interface ProductoMapper {
    
    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "id", ignore = true) // <--- Faltaba ignorar el ID
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "lotes", ignore = true)
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "id", ignore = true) // <--- Faltaba ignorar el ID
    @Mapping(target = "almacen", ignore = true)
    @Mapping(target = "lotes", ignore = true)
    void updateEntity(@MappingTarget Producto producto, ProductoRequest request);
}