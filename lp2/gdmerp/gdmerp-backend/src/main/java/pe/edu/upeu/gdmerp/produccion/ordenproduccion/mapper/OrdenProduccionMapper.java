package pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

@Mapper(componentModel = "spring")
public interface OrdenProduccionMapper {
    OrdenProduccionResponse toResponse(OrdenProduccion ordenProduccion);
    OrdenProduccion toEntity(OrdenProduccionRequest request);
    void updateEntity(@MappingTarget OrdenProduccion ordenProduccion, OrdenProduccionRequest request);
}