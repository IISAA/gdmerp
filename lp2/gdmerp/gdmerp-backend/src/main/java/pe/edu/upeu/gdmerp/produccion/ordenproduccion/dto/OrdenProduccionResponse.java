package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenProduccionResponse(
    Long id, 
    Long productoTerminadoId,
    String producto, 
    BigDecimal cantidadPlanificada, 
    EstadoOrdenProduccion estado,
    LocalDateTime fechaPlanificadaInicio,
    LocalDateTime fechaEstimadaFin,
    String loteGenerado,
    BigDecimal totalCostoInsumos,
    BigDecimal horasTrabajadas,
    BigDecimal costoOperativo,
    BigDecimal costoTotal,
    CentroTrabajoResponse centroTrabajo,
    List<DetalleOrdenResponse> detalles
) {
    public Long productoId() {
        return productoTerminadoId;
    }

    public BigDecimal cantidad() {
        return cantidadPlanificada;
    }

    public OrdenProduccionResponse(
        Long id,
        Long productoTerminadoId,
        String producto,
        BigDecimal cantidadPlanificada,
        EstadoOrdenProduccion estado,
        LocalDateTime fechaPlanificadaInicio,
        LocalDateTime fechaEstimadaFin,
        String loteGenerado,
        BigDecimal totalCostoInsumos,
        CentroTrabajoResponse centroTrabajo,
        List<DetalleOrdenResponse> detalles
    ) {
        this(
            id,
            productoTerminadoId,
            producto,
            cantidadPlanificada,
            estado,
            fechaPlanificadaInicio,
            fechaEstimadaFin,
            loteGenerado,
            totalCostoInsumos,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            centroTrabajo,
            detalles
        );
    }
}
