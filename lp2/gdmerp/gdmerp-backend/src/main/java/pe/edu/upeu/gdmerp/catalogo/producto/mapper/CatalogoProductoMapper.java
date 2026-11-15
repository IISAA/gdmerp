package pe.edu.upeu.gdmerp.catalogo.producto.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.catalogo.categoria.dto.CategoriaResumen;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CatalogoProductoMapper {

    @Mapping(target = "categoria", expression = "java(mapCategoria(producto.getCategoria()))")
    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "categoria", ignore = true)
    ProductoResponse toResponseSinCategoria(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Producto producto, ProductoRequest request);

    default CategoriaResumen mapCategoria(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaResumen(categoria.getId(), categoria.getNombre());
    }

    @AfterMapping
    default void applyImagenFallback(Producto source, @MappingTarget ProductoResponse target) {
        if (target.getImagenUrl() == null || target.getImagenUrl().isBlank()) {
            target.setImagenUrl("/assets/productos/producto-" + source.getId() + ".png");
        }
    }
}
