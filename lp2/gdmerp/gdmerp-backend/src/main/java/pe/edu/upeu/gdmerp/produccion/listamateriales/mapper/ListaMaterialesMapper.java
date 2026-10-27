package pe.edu.upeu.gdmerp.produccion.listamateriales.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;

@Mapper(componentModel = "spring")
public interface ListaMaterialesMapper {

    @Mapping(target = "productoTerminadoId", source = "productoTerminado.id")
    @Mapping(target = "nombreProductoTerminado", source = "productoTerminado.nombre")
    @Mapping(target = "skuProductoTerminado", source = "productoTerminado.sku")
    @Mapping(target = "unidadMedidaProductoTerminado", source = "productoTerminado.unidadMedida")
    ListaMaterialesResponse toResponse(ListaMateriales listaMateriales);

    @Mapping(target = "insumoId", source = "insumo.id")
    @Mapping(target = "nombreInsumo", source = "insumo.nombre")
    @Mapping(target = "skuInsumo", source = "insumo.sku")
    @Mapping(target = "unidadMedidaInsumo", source = "insumo.unidadMedida")
    @Mapping(target = "costoPromedioInsumo", source = "insumo.costoPromedio")
    DetalleListaMaterialesResponse toDetalleResponse(DetalleListaMateriales detalle);
}
