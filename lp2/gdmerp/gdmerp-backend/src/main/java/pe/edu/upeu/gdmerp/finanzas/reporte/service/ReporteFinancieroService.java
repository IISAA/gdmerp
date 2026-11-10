package pe.edu.upeu.gdmerp.finanzas.reporte.service;

import pe.edu.upeu.gdmerp.finanzas.reporte.dto.DashboardFinancieroResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.exporter.FormatoReporte;

public interface ReporteFinancieroService {

    DashboardFinancieroResponse obtenerDashboard();

    FlujoCajaResumenResponse obtenerFlujoCaja();

    ReporteRendicionesAgregadoDto obtenerReporteRendiciones();

    byte[] exportarCierreCaja(Long sesionId, FormatoReporte formato);
}
