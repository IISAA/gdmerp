package pe.edu.upeu.gdmerp.finanzas.reporte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.service.RendicionCajaChicaService;
import pe.edu.upeu.gdmerp.finanzas.exporter.CierreCajaExportService;
import pe.edu.upeu.gdmerp.finanzas.exporter.FormatoReporte;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.DashboardFinancieroResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.MovimientoCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.service.SesionCajaService;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.service.FinanzasService;

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
        List<MetodoPagoResumenDto> resumenMetodos = construirResumenMetodosPago(movimientoRepository.findAll());

        BigDecimal totalIngresosMovs = resumenMetodos.stream()
                .map(MetodoPagoResumenDto::getTotalIngresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEgresosMovs = resumenMetodos.stream()
                .map(MetodoPagoResumenDto::getTotalEgresos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoRendiciones = (rendiciones != null && rendiciones.getTotalMonto() != null)
                ? rendiciones.getTotalMonto()
                : BigDecimal.ZERO;

        BigDecimal granTotalIngresos = flujo.getTotalIngresos().add(totalIngresosMovs);
        BigDecimal granTotalEgresos = flujo.getTotalEgresos().add(totalEgresosMovs).add(montoRendiciones);

        Optional<SesionCaja> sesionActivaOpt = sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA);

        return DashboardFinancieroResponse.builder()
                .totalIngresos(granTotalIngresos)
                .totalEgresos(granTotalEgresos)
                .balanceNeto(granTotalIngresos.subtract(granTotalEgresos))
                .saldoCajaDiariaActiva(sesionActivaOpt.map(SesionCaja::getSaldoEsperadoEfectivo).orElse(BigDecimal.ZERO))
                .sesionActivaId(sesionActivaOpt.map(SesionCaja::getId).orElse(null))
                .codigoSesionActiva(sesionActivaOpt.map(SesionCaja::getCodigoSesion).orElse(null))
                .cajeroActivo(sesionActivaOpt.map(SesionCaja::getCajero).orElse(null))
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

    private List<MetodoPagoResumenDto> construirResumenMetodosPago(List<MovimientoCaja> movimientos) {
        Map<MetodoPago, BigDecimal[]> acumulador = new EnumMap<>(MetodoPago.class);
        for (MetodoPago mp : MetodoPago.values()) {
            acumulador.put(mp, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (MovimientoCaja m : movimientos) {
            BigDecimal[] par = acumulador.get(m.getMetodoPago());
            if (par == null || m.getMonto() == null) {
                continue;
            }
            if (m.getTipo() == TipoFinanciero.INGRESO) {
                par[0] = par[0].add(m.getMonto());
            } else if (m.getTipo() == TipoFinanciero.EGRESO) {
                par[1] = par[1].add(m.getMonto());
            }
        }

        List<MetodoPagoResumenDto> resumen = new ArrayList<>();
        for (Map.Entry<MetodoPago, BigDecimal[]> entry : acumulador.entrySet()) {
            BigDecimal ing = entry.getValue()[0];
            BigDecimal egr = entry.getValue()[1];
            resumen.add(MetodoPagoResumenDto.builder()
                    .metodoPago(entry.getKey())
                    .totalIngresos(ing)
                    .totalEgresos(egr)
                    .neto(ing.subtract(egr))
                    .build());
        }
        return resumen;
    }
}
