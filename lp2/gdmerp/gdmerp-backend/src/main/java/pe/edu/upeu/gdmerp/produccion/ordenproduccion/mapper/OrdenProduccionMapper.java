package pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper.CentroTrabajoMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

@Mapper(componentModel = "spring", uses = {CentroTrabajoMapper.class})
public interface OrdenProduccionMapper {

    @Mapping(target = "totalCostoInsumos", expression = "java(ordenProduccion.getTotalCostoInsumos())")
    OrdenProduccionResponse toResponse(OrdenProduccion ordenProduccion);

    @Mapping(target = "subtotal", expression = "java(detalle.getSubtotal())")
    DetalleOrdenResponse toDetalleResponse(DetalleOrdenProduccion detalle);

    @Mapping(target = "centroTrabajo", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    OrdenProduccion toEntity(OrdenProduccionRequest request);

    @Mapping(target = "centroTrabajo", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    void updateEntity(@MappingTarget OrdenProduccion ordenProduccion, OrdenProduccionRequest request);
}