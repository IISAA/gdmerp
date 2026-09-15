package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ReporteConsumoOrdenDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import java.util.List;

public interface OrdenProduccionReporteService {
    List<ResumenEstadoOrdenesDTO> obtenerResumenEstados();
    ReporteConsumoOrdenDTO calcularConsumo(Long ordenId);
}