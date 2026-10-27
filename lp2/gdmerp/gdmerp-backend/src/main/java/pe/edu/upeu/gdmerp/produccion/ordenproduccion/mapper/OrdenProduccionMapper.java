package pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper.CentroTrabajoMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleOrdenResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

@Mapper(componentModel = "spring", uses = {CentroTrabajoMapper.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrdenProduccionMapper {

    @Mapping(target = "totalCostoInsumos", expression = "java(ordenProduccion.getTotalCostoInsumos())")
    OrdenProduccionResponse toResponse(OrdenProduccion ordenProduccion);

    @Mapping(target = "nombreMateriaPrima", source = "nombreProducto")
    @Mapping(target = "subtotal", expression = "java(detalle.getSubtotal())")
    DetalleOrdenResponse toDetalleResponse(DetalleOrdenProduccion detalle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "productoTerminadoId", source = "productoTerminadoId")
    @Mapping(target = "centroTrabajo.id", source = "centroTrabajoId")
    @Mapping(target = "loteGenerado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "cantidad", ignore = true)
    OrdenProduccion toEntity(OrdenProduccionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "productoTerminadoId", source = "productoTerminadoId")
    @Mapping(target = "centroTrabajo.id", source = "centroTrabajoId")
    @Mapping(target = "loteGenerado", ignore = true)
    @Mapping(target = "detalles", ignore = true)
    @Mapping(target = "cantidad", ignore = true)
    void updateEntity(@MappingTarget OrdenProduccion ordenProduccion, OrdenProduccionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ordenProduccion", ignore = true)
    @Mapping(target = "materiaPrimaId", source = "materiaPrimaId")
    @Mapping(target = "insumo.id", source = "materiaPrimaId")
    @Mapping(target = "cantidadRequerida", source = "cantidadRequerida")
    @Mapping(target = "nombreProducto", ignore = true)
    @Mapping(target = "costoUnitario", ignore = true)
    @Mapping(target = "cantidadConsumida", ignore = true)
    DetalleOrdenProduccion aEntidad(DetalleOrdenRequest dto);

    default DetalleOrdenProduccion toDetalleEntity(DetalleOrdenRequest dto) {
        return aEntidad(dto);
    }
}
