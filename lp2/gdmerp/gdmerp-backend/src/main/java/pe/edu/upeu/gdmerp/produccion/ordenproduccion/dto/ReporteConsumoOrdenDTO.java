package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteConsumoOrdenDTO(
    Long ordenId,
    String producto,
    Integer cantidadPlanificada,
    String estado,
    BigDecimal costoTotalMateriales,
    List<DetalleConsumoDTO> materiales
) {}