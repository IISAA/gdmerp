package pe.edu.upeu.gdmerp.inventario.categoria.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaRequest;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {
    CategoriaResponse toResponse(Categoria categoria);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productos", ignore = true)
    Categoria toEntity(CategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productos", ignore = true)
    void updateEntity(@MappingTarget Categoria categoria, CategoriaRequest request);
}
