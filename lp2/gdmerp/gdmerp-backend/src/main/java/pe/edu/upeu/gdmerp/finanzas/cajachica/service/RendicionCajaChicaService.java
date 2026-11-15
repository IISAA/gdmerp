package pe.edu.upeu.gdmerp.finanzas.cajachica.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RendicionCajaChicaService {

    default CajaChicaResponse crearCajaChica(
            String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial) {
        return crearCajaChica(codigo, responsable, limiteMaximo, saldoInicial, null);
    }

    CajaChicaResponse crearCajaChica(
            String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial, Long centroCostoId);

    CajaChicaResponse obtenerCajaChica(Long id);

    RendicionCajaChicaResponse registrarRendicion(RendicionCajaChicaRequest request);

    RendicionCajaChicaResponse obtenerRendicion(Long id);

    List<RendicionCajaChicaResponse> listarRendicionesPorCaja(Long cajaChicaId);

    Page<RendicionCajaChicaResponse> listarPaginado(Pageable pageable);

    Page<RendicionCajaChicaResponse> buscarConFiltros(
            Long cajaChicaId,
            EstadoRendicion estado,
            String responsable,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax,
            Pageable pageable
    );

    List<CajaChicaResponse> listarCajasChicas();

    RendicionCajaChicaResponse anularRendicion(Long id);

    List<RendicionResumenDto> obtenerResumen(Long cajaChicaId);

    ReporteRendicionesAgregadoDto obtenerReporteAgregado(Long cajaChicaId);

    List<CentroCostoResponse> listarCentrosCosto();

    CentroCostoResponse crearCentroCosto(CentroCostoRequest request);
}

