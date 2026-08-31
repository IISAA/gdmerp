package pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoRequest;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;

@Mapper(componentModel = "spring")
public interface CentroTrabajoMapper {
    CentroTrabajoResponse toResponse(CentroTrabajo centroTrabajo);
    CentroTrabajo toEntity(CentroTrabajoRequest request);
    void updateEntity(@MappingTarget CentroTrabajo centroTrabajo, CentroTrabajoRequest request);
}