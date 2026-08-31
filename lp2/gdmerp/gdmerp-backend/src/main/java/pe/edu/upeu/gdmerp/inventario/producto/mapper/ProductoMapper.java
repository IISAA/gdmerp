package pe.edu.upeu.gdmerp.inventario.producto.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

@Mapper(componentModel = "spring")
public interface ProductoMapper {
    ProductoResponse toResponse(Producto producto);
    Producto toEntity(ProductoRequest request);
    void updateEntity(@MappingTarget Producto producto, ProductoRequest request);
}