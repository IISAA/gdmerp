package pe.edu.upeu.gdmerp.finanzas.reporte.dto;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardFinancieroResponse {
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal balanceNeto;
    private BigDecimal saldoCajaDiariaActiva;
    private Long sesionActivaId;
    private String codigoSesionActiva;
    private String cajeroActivo;
    private List<MetodoPagoResumenDto> resumenPorMetodo;
    private FlujoCajaResumenResponse flujoCaja;
    private ReporteRendicionesAgregadoDto reporteRendiciones;
}
