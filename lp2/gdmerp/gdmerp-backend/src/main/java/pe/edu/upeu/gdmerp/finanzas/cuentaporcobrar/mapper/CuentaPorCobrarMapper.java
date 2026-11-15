package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.mapper;

import org.mapstruct.Context;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.AbonoResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.AbonoCuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CuentaPorCobrarMapper {

    @Named("toCuentaPorCobrarWithCount")
    @Mapping(target = "montoPagado", expression = "java(calcularMontoPagado(entity))")
    @Mapping(target = "porcentajePagado", expression = "java(calcularPorcentajePagado(entity))")
    @Mapping(target = "cantidadAbonos", expression = "java(resolveCantidadAbonos(entity.getId(), abonosCountByCuenta))")
    CuentaPorCobrarResponse toResponse(CuentaPorCobrar entity, @Context Map<Long, Integer> abonosCountByCuenta);

    @Named("toCuentaPorCobrarSummary")
    @Mapping(target = "montoPagado", expression = "java(calcularMontoPagado(entity))")
    @Mapping(target = "porcentajePagado", expression = "java(calcularPorcentajePagado(entity))")
    @Mapping(target = "cantidadAbonos", constant = "0")
    CuentaPorCobrarResponse toSummaryResponse(CuentaPorCobrar entity);

    @IterableMapping(qualifiedByName = "toCuentaPorCobrarWithCount")
    List<CuentaPorCobrarResponse> toResponseList(List<CuentaPorCobrar> entities, @Context Map<Long, Integer> abonosCountByCuenta);

    @Mapping(target = "montoPagado", expression = "java(calcularMontoPagado(entity))")
    @Mapping(target = "porcentajePagado", expression = "java(calcularPorcentajePagado(entity))")
    @Mapping(target = "abonos", ignore = true)
    CuentaPorCobrarDetalleResponse toListDetalleResponse(CuentaPorCobrar entity);

    @Mapping(target = "montoPagado", expression = "java(calcularMontoPagado(entity))")
    @Mapping(target = "porcentajePagado", expression = "java(calcularPorcentajePagado(entity))")
    CuentaPorCobrarDetalleResponse toDetalleResponse(CuentaPorCobrar entity);

    @Mapping(target = "cuentaPorCobrarId", source = "cuentaPorCobrar.id")
    AbonoResponse toAbonoResponse(AbonoCuentaPorCobrar entity);

    default BigDecimal calcularMontoPagado(CuentaPorCobrar entity) {
        if (entity == null || entity.getMontoTotal() == null || entity.getSaldoPendiente() == null) {
            return BigDecimal.ZERO;
        }
        return entity.getMontoTotal().subtract(entity.getSaldoPendiente());
    }

    default Double calcularPorcentajePagado(CuentaPorCobrar entity) {
        if (entity == null || entity.getMontoTotal() == null || entity.getMontoTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return calcularMontoPagado(entity)
                .multiply(BigDecimal.valueOf(100))
                .divide(entity.getMontoTotal(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    default int resolveCantidadAbonos(Long cuentaId, Map<Long, Integer> abonosCountByCuenta) {
        if (cuentaId == null || abonosCountByCuenta == null) {
            return 0;
        }
        return abonosCountByCuenta.getOrDefault(cuentaId, 0);
    }
}
