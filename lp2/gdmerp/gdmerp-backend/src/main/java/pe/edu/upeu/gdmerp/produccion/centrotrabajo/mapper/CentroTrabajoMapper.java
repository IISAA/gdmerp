package pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoRequest;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CentroTrabajoMapper {

    CentroTrabajoResponse toResponse(CentroTrabajo centroTrabajo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ordenes", ignore = true)
    CentroTrabajo toEntity(CentroTrabajoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ordenes", ignore = true)
    void updateEntity(@MappingTarget CentroTrabajo centroTrabajo, CentroTrabajoRequest request);
}