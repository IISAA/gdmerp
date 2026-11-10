package pe.edu.upeu.gdmerp.finanzas.reporte.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class FlujoCajaResumenResponse {
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal balanceNeto;
    private BigDecimal cuentasPorCobrarPendientes;
    private BigDecimal cuentasPorPagarPendientes;
    private long totalTransacciones;

    public FlujoCajaResumenResponse() {
    }

    public FlujoCajaResumenResponse(BigDecimal totalIngresos, BigDecimal totalEgresos, BigDecimal balanceNeto, BigDecimal cuentasPorCobrarPendientes, BigDecimal cuentasPorPagarPendientes, long totalTransacciones) {
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.balanceNeto = balanceNeto;
        this.cuentasPorCobrarPendientes = cuentasPorCobrarPendientes;
        this.cuentasPorPagarPendientes = cuentasPorPagarPendientes;
        this.totalTransacciones = totalTransacciones;
    }
}
