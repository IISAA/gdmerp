package pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper.CentroTrabajoMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

@Mapper(componentModel = "spring", uses = {CentroTrabajoMapper.class})
public interface OrdenProduccionMapper {
    OrdenProduccionResponse toResponse(OrdenProduccion ordenProduccion);

    @Mapping(target = "centroTrabajo", ignore = true)
    OrdenProduccion toEntity(OrdenProduccionRequest request);

    @Mapping(target = "centroTrabajo", ignore = true)
    void updateEntity(@MappingTarget OrdenProduccion ordenProduccion, OrdenProduccionRequest request);
}
