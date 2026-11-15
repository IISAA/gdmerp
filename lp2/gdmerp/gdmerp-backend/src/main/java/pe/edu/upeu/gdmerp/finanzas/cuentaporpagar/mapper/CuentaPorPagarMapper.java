package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.mapper;

import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.AbonoCuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.AbonoCuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.CuentaPorPagar;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CuentaPorPagarMapper {

    @Named("toCuentaPorPagarWithCount")
    @Mapping(target = "cantidadAbonos", expression = "java(resolveCantidadAbonos(entity.getId(), abonosCountByCuenta))")
    CuentaPorPagarResponse toResponse(CuentaPorPagar entity, @Context Map<Long, Integer> abonosCountByCuenta);

    @Named("toCuentaPorPagarSummary")
    @Mapping(target = "cantidadAbonos", constant = "0")
    CuentaPorPagarResponse toSummaryResponse(CuentaPorPagar entity);

    @IterableMapping(qualifiedByName = "toCuentaPorPagarWithCount")
    List<CuentaPorPagarResponse> toResponseList(List<CuentaPorPagar> entities, @Context Map<Long, Integer> abonosCountByCuenta);

    @Mapping(target = "abonos", ignore = true)
    CuentaPorPagarDetalleResponse toListDetalleResponse(CuentaPorPagar entity);

    CuentaPorPagarDetalleResponse toDetalleResponse(CuentaPorPagar entity);

    @Mapping(target = "cuentaPorPagarId", source = "cuentaPorPagar.id")
    AbonoCuentaPorPagarResponse toAbonoResponse(AbonoCuentaPorPagar abono);

    default Integer resolveCantidadAbonos(Long cuentaId, Map<Long, Integer> abonosCountByCuenta) {
        if (cuentaId == null || abonosCountByCuenta == null) {
            return 0;
        }
        return abonosCountByCuenta.getOrDefault(cuentaId, 0);
    }
}
