package pe.edu.upeu.gdmerp.inventario.unidadmedida.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaRequest;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.dto.UnidadMedidaResponse;
import pe.edu.upeu.gdmerp.inventario.unidadmedida.entity.UnidadMedida;

@Mapper(componentModel = "spring")
public interface UnidadMedidaMapper {

    UnidadMedidaResponse toResponse(UnidadMedida unidadMedida);

    @Mapping(target = "id", ignore = true)
    UnidadMedida toEntity(UnidadMedidaRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(@MappingTarget UnidadMedida unidadMedida, UnidadMedidaRequest request);
}
