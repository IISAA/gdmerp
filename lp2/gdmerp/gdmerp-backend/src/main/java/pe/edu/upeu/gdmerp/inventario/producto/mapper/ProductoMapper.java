package pe.edu.upeu.gdmerp.inventario.producto.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.almacen.mapper.AlmacenMapper;
import pe.edu.upeu.gdmerp.inventario.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

// El "uses" permite a MapStruct saber cómo convertir Almacen y Categoria a sus Responses
@Mapper(componentModel = "spring", uses = {AlmacenMapper.class, CategoriaMapper.class})
public interface ProductoMapper {
    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "almacen", ignore = true) // El servicio asignará la entidad
    @Mapping(target = "categoria", ignore = true) // El servicio asignará la entidad
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "almacen", ignore = true) // El servicio asignará la entidad
    @Mapping(target = "categoria", ignore = true) // El servicio asignará la entidad
    void updateEntity(@MappingTarget Producto producto, ProductoRequest request);
}
