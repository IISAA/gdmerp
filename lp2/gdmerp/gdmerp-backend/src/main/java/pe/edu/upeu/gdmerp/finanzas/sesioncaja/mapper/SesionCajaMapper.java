package pe.edu.upeu.gdmerp.finanzas.sesioncaja.mapper;

import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DesgloseEfectivoItemDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SesionCajaMapper {

    @Mapping(target = "categoriaFinancieraId", source = "categoriaFinanciera.id")
    @Mapping(target = "categoriaFinancieraNombre", source = "categoriaFinanciera.nombre")
    @Mapping(target = "sesionCajaId", source = "sesionCaja.id")
    MovimientoCajaResponse toMovimientoResponse(MovimientoCaja entity);

    List<MovimientoCajaResponse> toMovimientoResponseList(List<MovimientoCaja> entities);

    DenominacionMonedaResponse toDenominacionResponse(DenominacionMoneda entity);

    List<DenominacionMonedaResponse> toDenominacionResponseList(List<DenominacionMoneda> entities);

    @Mapping(target = "denominacionId", source = "denominacionMoneda.id")
    @Mapping(target = "valorFacial", source = "denominacionMoneda.valorFacial")
    @Mapping(target = "tipo", source = "denominacionMoneda.tipo")
    DesgloseEfectivoItemDto toDesgloseResponse(DetalleArqueoEfectivo entity);

    @Named("toSesionWithCount")
    @Mapping(target = "cantidadMovimientos", expression = "java(resolveCantidadMovimientos(entity.getId(), movimientosCountBySesion))")
    SesionCajaResponse toResponse(SesionCaja entity, @Context Map<Long, Integer> movimientosCountBySesion);

    @Named("toSesionSummary")
    @Mapping(target = "cantidadMovimientos", constant = "0")
    SesionCajaResponse toSummaryResponse(SesionCaja entity);

    @IterableMapping(qualifiedByName = "toSesionWithCount")
    List<SesionCajaResponse> toResponseList(List<SesionCaja> entities, @Context Map<Long, Integer> movimientosCountBySesion);

    @Mapping(target = "movimientos", ignore = true)
    @Mapping(target = "detallesArqueo", ignore = true)
    SesionCajaDetalleResponse toListDetalleResponse(SesionCaja entity);

    SesionCajaDetalleResponse toDetalleResponse(SesionCaja entity);

    default int resolveCantidadMovimientos(Long sesionId, Map<Long, Integer> movimientosCountBySesion) {
        if (sesionId == null || movimientosCountBySesion == null) {
            return 0;
        }
        return movimientosCountBySesion.getOrDefault(sesionId, 0);
    }
}
