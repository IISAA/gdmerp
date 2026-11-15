package pe.edu.upeu.gdmerp.produccion.listamateriales.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.DetalleListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.DetalleListaMateriales;
import pe.edu.upeu.gdmerp.produccion.listamateriales.entity.ListaMateriales;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ListaMaterialesMapper {

    @Mapping(target = "id", source = "listaMateriales.id")
    @Mapping(target = "codigo", source = "listaMateriales.codigo")
    @Mapping(target = "productoTerminadoId", source = "listaMateriales.productoTerminadoId")
    @Mapping(target = "nombreProductoTerminado", source = "productoTerminado.nombre")
    @Mapping(target = "skuProductoTerminado", source = "productoTerminado.sku")
    @Mapping(target = "unidadMedidaProductoTerminado", source = "productoTerminado.unidadMedida")
    @Mapping(target = "cantidadBase", source = "listaMateriales.cantidadBase")
    @Mapping(target = "detalles", source = "detalles")
    ListaMaterialesResponse toResponse(
            ListaMateriales listaMateriales,
            ProductoResponse productoTerminado,
            List<DetalleListaMaterialesResponse> detalles
    );

    @Mapping(target = "id", source = "listaMateriales.id")
    @Mapping(target = "codigo", source = "listaMateriales.codigo")
    @Mapping(target = "productoTerminadoId", source = "listaMateriales.productoTerminadoId")
    @Mapping(target = "nombreProductoTerminado", source = "productoTerminado.nombre")
    @Mapping(target = "skuProductoTerminado", source = "productoTerminado.sku")
    @Mapping(target = "unidadMedidaProductoTerminado", source = "productoTerminado.unidadMedida")
    @Mapping(target = "cantidadBase", source = "listaMateriales.cantidadBase")
    @Mapping(target = "detalles", ignore = true)
    ListaMaterialesResponse toListResponse(
            ListaMateriales listaMateriales,
            ProductoResponse productoTerminado
    );

    @Mapping(target = "id", source = "detalle.id")
    @Mapping(target = "insumoId", source = "detalle.insumoId")
    @Mapping(target = "nombreInsumo", source = "insumo.nombre")
    @Mapping(target = "skuInsumo", source = "insumo.sku")
    @Mapping(target = "unidadMedidaInsumo", source = "insumo.unidadMedida")
    @Mapping(target = "cantidadRequerida", source = "detalle.cantidadRequerida")
    @Mapping(target = "costoPromedioInsumo", source = "costoPromedio")
    DetalleListaMaterialesResponse toDetalleResponse(
            DetalleListaMateriales detalle,
            ProductoResponse insumo,
            BigDecimal costoPromedio
    );
}
