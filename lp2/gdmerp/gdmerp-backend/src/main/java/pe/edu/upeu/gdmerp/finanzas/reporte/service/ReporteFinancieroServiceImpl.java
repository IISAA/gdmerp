package pe.edu.upeu.gdmerp.finanzas.reporte.service;

import pe.edu.upeu.gdmerp.finanzas.transaccion.service.FinanzasService;
import pe.edu.upeu.gdmerp.finanzas.cajachica.service.RendicionCajaChicaService;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.service.SesionCajaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.DashboardFinancieroResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.exporter.CierreCajaExportService;
import pe.edu.upeu.gdmerp.finanzas.exporter.FormatoReporte;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.MovimientoCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReporteFinancieroServiceImpl implements ReporteFinancieroService {

    private final FinanzasService finanzasService;
    private final RendicionCajaChicaService rendicionCajaChicaService;
    private final SesionCajaService sesionCajaService;
    private final SesionCajaRepository sesionRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final CierreCajaExportService exportService;

    public ReporteFinancieroServiceImpl(
            FinanzasService finanzasService,
            RendicionCajaChicaService rendicionCajaChicaService,
            SesionCajaService sesionCajaService,
            SesionCajaRepository sesionRepository,
            MovimientoCajaRepository movimientoRepository,
            CierreCajaExportService exportService) {
        this.finanzasService = finanzasService;
        this.rendicionCajaChicaService = rendicionCajaChicaService;
        this.sesionCajaService = sesionCajaService;
        this.sesionRepository = sesionRepository;
        this.movimientoRepository = movimientoRepository;
        this.exportService = exportService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardFinancieroResponse obtenerDashboard() {
        FlujoCajaResumenResponse flujo = finanzasService.obtenerResumenFlujoCaja();
        ReporteRendicionesAgregadoDto rendiciones = rendicionCajaChicaService.obtenerReporteAgregado(null);

        // Agregación de movimientos de todas las sesiones de caja
        List<MovimientoCaja> todosMovs = movimientoRepository.findAll();
        Map<MetodoPago, BigDecimal[]> acumulador = new EnumMap<>(MetodoPago.class);
        for (MetodoPago mp : MetodoPago.values()) {
            acumulador.put(mp, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        BigDecimal totalIngresosMovs = BigDecimal.ZERO;
        BigDecimal totalEgresosMovs = BigDecimal.ZERO;

        for (MovimientoCaja m : todosMovs) {
            BigDecimal[] par = acumulador.get(m.getMetodoPago());
            if (par != null && m.getMonto() != null) {
                if (m.getTipo() == TipoFinanciero.INGRESO) {
                    par[0] = par[0].add(m.getMonto());
                    totalIngresosMovs = totalIngresosMovs.add(m.getMonto());
                } else if (m.getTipo() == TipoFinanciero.EGRESO) {
                    par[1] = par[1].add(m.getMonto());
                    totalEgresosMovs = totalEgresosMovs.add(m.getMonto());
                }
            }
        }

        List<MetodoPagoResumenDto> resumenMetodos = new ArrayList<>();
        for (Map.Entry<MetodoPago, BigDecimal[]> entry : acumulador.entrySet()) {
            BigDecimal ing = entry.getValue()[0];
            BigDecimal egr = entry.getValue()[1];
            resumenMetodos.add(MetodoPagoResumenDto.builder()
                    .metodoPago(entry.getKey())
                    .totalIngresos(ing)
                    .totalEgresos(egr)
                    .neto(ing.subtract(egr))
                    .build());
        }

        // Consolidación de ingresos y egresos de Transacciones + Movimientos de Caja Diaria
        BigDecimal granTotalIngresos = flujo.getTotalIngresos().add(totalIngresosMovs);
        BigDecimal granTotalEgresos = flujo.getTotalEgresos().add(totalEgresosMovs).add(
                rendiciones != null && rendiciones.getTotalMonto() != null ? rendiciones.getTotalMonto() : BigDecimal.ZERO
        );
        BigDecimal balanceNeto = granTotalIngresos.subtract(granTotalEgresos);

        // Turno activo
        Optional<SesionCaja> sesionActivaOpt = sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA);
        Long sesionActivaId = sesionActivaOpt.map(SesionCaja::getId).orElse(null);
        String codSesion = sesionActivaOpt.map(SesionCaja::getCodigoSesion).orElse(null);
        String cajero = sesionActivaOpt.map(SesionCaja::getCajero).orElse(null);
        BigDecimal saldoActivo = sesionActivaOpt.map(SesionCaja::getSaldoEsperadoEfectivo).orElse(BigDecimal.ZERO);

        return DashboardFinancieroResponse.builder()
                .totalIngresos(granTotalIngresos)
                .totalEgresos(granTotalEgresos)
                .balanceNeto(balanceNeto)
                .saldoCajaDiariaActiva(saldoActivo)
                .sesionActivaId(sesionActivaId)
                .codigoSesionActiva(codSesion)
                .cajeroActivo(cajero)
                .resumenPorMetodo(resumenMetodos)
                .flujoCaja(flujo)
                .reporteRendiciones(rendiciones)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FlujoCajaResumenResponse obtenerFlujoCaja() {
        return finanzasService.obtenerResumenFlujoCaja();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteRendicionesAgregadoDto obtenerReporteRendiciones() {
        return rendicionCajaChicaService.obtenerReporteAgregado(null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportarCierreCaja(Long sesionId, FormatoReporte formato) {
        CierreCajaReporteDto reporteDto = sesionCajaService.generarDatosReporte(sesionId);
        return exportService.exportar(formato, reporteDto);
    }
}
