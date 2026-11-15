package pe.edu.upeu.gdmerp.finanzas.cajachica.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.DetalleRendicionCajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.RendicionCajaChica;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CajaChicaMapper {

    CajaChicaResponse toCajaChicaResponse(CajaChica entity);

    @Named("toCajaChicaListItem")
    @Mapping(target = "custodiosAutorizados", ignore = true)
    CajaChicaResponse toCajaChicaListResponse(CajaChica entity);

    @IterableMapping(qualifiedByName = "toCajaChicaListItem")
    List<CajaChicaResponse> toCajaChicaResponseList(List<CajaChica> entities);

    CentroCostoResponse toCentroCostoResponse(CentroCosto entity);

    List<CentroCostoResponse> toCentroCostoResponseList(List<CentroCosto> entities);

    @Mapping(target = "centroCostoId", source = "centroCosto.id")
    @Mapping(target = "centroCostoCodigo", source = "centroCosto.codigoCc")
    @Mapping(target = "centroCostoNombre", source = "centroCosto.nombreArea")
    DetalleRendicionResponse toDetalleResponse(DetalleRendicionCajaChica entity);

    @Mapping(target = "cajaChicaId", source = "cajaChica.id")
    RendicionCajaChicaResponse toRendicionResponse(RendicionCajaChica entity);

    @Named("toRendicionListItem")
    @Mapping(target = "cajaChicaId", source = "cajaChica.id")
    @Mapping(target = "detalles", ignore = true)
    RendicionCajaChicaResponse toRendicionListResponse(RendicionCajaChica entity);

    @IterableMapping(qualifiedByName = "toRendicionListItem")
    List<RendicionCajaChicaResponse> toRendicionList(List<RendicionCajaChica> entities);
}
