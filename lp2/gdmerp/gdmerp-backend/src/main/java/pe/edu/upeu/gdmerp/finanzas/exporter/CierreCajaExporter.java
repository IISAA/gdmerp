package pe.edu.upeu.gdmerp.finanzas.exporter;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;

/**
 * Contrato polimórfico de exportación de cortes de caja y balances (OCP / DIP).
 * Permite agregar nuevos formatos (CSV, HTML, JSON) sin modificar el código cliente.
 */
public interface CierreCajaExporter {

    boolean supports(FormatoReporte formato);

    byte[] exportarCierreCaja(CierreCajaReporteDto datos);

    String getContentType();

    String getFileExtension();
}
