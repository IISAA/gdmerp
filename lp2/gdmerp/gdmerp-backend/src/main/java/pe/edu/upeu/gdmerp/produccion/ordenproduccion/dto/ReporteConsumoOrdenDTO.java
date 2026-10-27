package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import java.math.BigDecimal;
import java.util.List;

public record ReporteConsumoOrdenDTO(
    Long ordenId,
    String producto,
    BigDecimal cantidadPlanificada,
    EstadoOrdenProduccion estado,
    BigDecimal costoTotalMateriales,
    List<DetalleConsumoDTO> materiales
) {
    public ReporteConsumoOrdenDTO(Long ordenId, String producto, Integer cantidadPlanificada, EstadoOrdenProduccion estado, BigDecimal costoTotalMateriales, List<DetalleConsumoDTO> materiales) {
        this(
            ordenId,
            producto,
            cantidadPlanificada != null ? BigDecimal.valueOf(cantidadPlanificada) : BigDecimal.ZERO,
            estado,
            costoTotalMateriales,
            materiales
        );
    }
}
