package pe.edu.upeu.gdmerp.finanzas.cajachica.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.AsignacionCajaCentroCostoResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.AsignacionCajaCentroCostoId;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.DetalleRendicionCajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.RendicionCajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.AsignacionCajaCentroCostoRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CentroCostoRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.RendicionCajaChicaRepository;
import pe.edu.upeu.gdmerp.shared.event.ValidacionProductoSolicitadaEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class RendicionCajaChicaServiceImpl implements RendicionCajaChicaService {

    private final CajaChicaRepository cajaChicaRepository;
    private final RendicionCajaChicaRepository rendicionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CentroCostoRepository centroCostoRepository;
    private final AsignacionCajaCentroCostoRepository asignacionRepository;

    public RendicionCajaChicaServiceImpl(
            CajaChicaRepository cajaChicaRepository,
            RendicionCajaChicaRepository rendicionRepository,
            ApplicationEventPublisher eventPublisher,
            CentroCostoRepository centroCostoRepository,
            AsignacionCajaCentroCostoRepository asignacionRepository) {
        this.cajaChicaRepository = cajaChicaRepository;
        this.rendicionRepository = rendicionRepository;
        this.eventPublisher = eventPublisher;
        this.centroCostoRepository = centroCostoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Override
    @Transactional
    public CajaChicaResponse crearCajaChica(String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial) {
        if (cajaChicaRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una caja chica con el código: " + codigo);
        }

        CajaChica caja = CajaChica.builder()
                .codigo(codigo)
                .responsable(responsable)
                .limiteMaximo(limiteMaximo)
                .saldoDisponible(saldoInicial)
                .build();

        return mapToCajaChicaResponse(cajaChicaRepository.save(caja));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaChicaResponse> listarCajasChicas() {
        return cajaChicaRepository.findAll().stream()
                .map(this::mapToCajaChicaResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CajaChicaResponse obtenerCajaChica(Long id) {
        return mapToCajaChicaResponse(buscarCajaChicaOFallar(id));
    }

    @Override
    @Transactional
    public RendicionCajaChicaResponse anularRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));

        if (rendicion.getEstado() == EstadoRendicion.ANULADO) {
            throw new IllegalStateException("RN-FIN-02: La rendición con código " + rendicion.getCodigo()
                    + " ya se encuentra en estado ANULADO y no se puede anular nuevamente.");
        }

        rendicion.setEstado(EstadoRendicion.ANULADO);
        CajaChica caja = rendicion.getCajaChica();
        caja.setSaldoDisponible(caja.getSaldoDisponible().add(rendicion.getMontoTotal()));
        cajaChicaRepository.save(caja);
        reintegrarSaldosPorCentroCosto(rendicion, caja);

        return mapToRendicionResponse(rendicionRepository.save(rendicion));
    }

    @Override
    @Transactional
    public RendicionCajaChicaResponse registrarRendicion(RendicionCajaChicaRequest request) {
        if (rendicionRepository.existsByCodigo(request.getCodigo())) {
            throw new IllegalArgumentException("Ya existe una rendición con el código: " + request.getCodigo());
        }

        CajaChica cajaChica = buscarCajaChicaOFallar(request.getCajaChicaId());
        RendicionCajaChica rendicion = RendicionCajaChica.builder()
                .codigo(request.getCodigo())
                .responsable(request.getResponsable())
                .fecha(LocalDate.now())
                .estado(EstadoRendicion.APROBADO)
                .cajaChica(cajaChica)
                .build();

        for (DetalleRendicionRequest detReq : request.getDetalles()) {
            rendicion.agregarDetalle(construirYValidarDetalleRendicion(detReq, cajaChica));
        }

        rendicion.calcularTotal();
        validarYDescontarSolvenciaCaja(cajaChica, rendicion.getMontoTotal());

        cajaChicaRepository.save(cajaChica);
        return mapToRendicionResponse(rendicionRepository.save(rendicion));
    }

    @Override
    @Transactional(readOnly = true)
    public RendicionCajaChicaResponse obtenerRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));
        return mapToRendicionResponse(rendicion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendicionCajaChicaResponse> listarRendicionesPorCaja(Long cajaChicaId) {
        buscarCajaChicaOFallar(cajaChicaId);
        return rendicionRepository.findByCajaChicaId(cajaChicaId).stream()
                .map(this::mapToRendicionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RendicionCajaChicaResponse> listarPaginado(Pageable pageable) {
        return rendicionRepository.findAll(pageable).map(this::mapToRendicionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RendicionCajaChicaResponse> buscarConFiltros(
            Long cajaChicaId,
            EstadoRendicion estado,
            String responsable,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal montoMin,
            BigDecimal montoMax,
            Pageable pageable) {
        return rendicionRepository.buscarConFiltros(
                cajaChicaId, estado, responsable, fechaInicio, fechaFin, montoMin, montoMax, pageable
        ).map(this::mapToRendicionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendicionResumenDto> obtenerResumen(Long cajaChicaId) {
        if (cajaChicaId != null) {
            buscarCajaChicaOFallar(cajaChicaId);
        }
        return rendicionRepository.obtenerResumenProyectado(cajaChicaId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteRendicionesAgregadoDto obtenerReporteAgregado(Long cajaChicaId) {
        if (cajaChicaId != null) {
            buscarCajaChicaOFallar(cajaChicaId);
        }
        ReporteRendicionesAgregadoDto reporte = rendicionRepository.obtenerReporteAgregado(cajaChicaId);
        return reporte != null ? reporte : ReporteRendicionesAgregadoDto.empty();
    }

    @Override
    @Transactional
    public AsignacionCajaCentroCostoResponse asignarPresupuestoCentroCosto(
            Long cajaChicaId, Long centroCostoId, BigDecimal montoAsignado) {
        CajaChica caja = buscarCajaChicaOFallar(cajaChicaId);
        CentroCosto cc = centroCostoRepository.findById(centroCostoId)
                .orElseThrow(() -> new ResourceNotFoundException("Centro de costo no encontrado con id: " + centroCostoId));

        Optional<AsignacionCajaCentroCosto> existente = buscarAsignacionPorCentroCosto(caja, centroCostoId);
        AsignacionCajaCentroCosto asignacion;

        if (existente.isPresent()) {
            asignacion = existente.get();
            BigDecimal diferencia = montoAsignado.subtract(asignacion.getPresupuestoAsignado());
            BigDecimal nuevoSaldo = asignacion.getSaldoDisponible().add(diferencia);
            if (nuevoSaldo.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("No se puede reducir el presupuesto por debajo de lo ya consumido");
            }
            asignacion.setPresupuestoAsignado(montoAsignado);
            asignacion.setSaldoDisponible(nuevoSaldo);
            asignacion.setActivo(true);
        } else {
            asignacion = AsignacionCajaCentroCosto.builder()
                    .id(new AsignacionCajaCentroCostoId(cajaChicaId, centroCostoId))
                    .cajaChica(caja)
                    .centroCosto(cc)
                    .presupuestoAsignado(montoAsignado)
                    .saldoDisponible(montoAsignado)
                    .fechaAsignacion(LocalDate.now())
                    .activo(true)
                    .build();
            caja.agregarAsignacion(asignacion);
        }

        cajaChicaRepository.save(caja);
        return mapToAsignacionResponse(asignacion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AsignacionCajaCentroCostoResponse> listarPresupuestosPorCaja(Long cajaChicaId) {
        buscarCajaChicaOFallar(cajaChicaId);
        return asignacionRepository.findByCajaChicaId(cajaChicaId).stream()
                .map(this::mapToAsignacionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CentroCostoResponse> listarCentrosCosto() {
        return centroCostoRepository.findByActivoTrueOrderByCodigoCcAsc().stream()
                .map(c -> CentroCostoResponse.builder()
                        .id(c.getId())
                        .codigoCc(c.getCodigoCc())
                        .nombreArea(c.getNombreArea())
                        .activo(c.getActivo())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public CentroCostoResponse crearCentroCosto(CentroCostoRequest request) {
        String codigoLimpio = request.getCodigoCc().trim().toUpperCase();
        if (centroCostoRepository.existsByCodigoCc(codigoLimpio)) {
            throw new IllegalArgumentException("Ya existe un centro de costo con el código: " + codigoLimpio);
        }
        CentroCosto cc = centroCostoRepository.save(CentroCosto.builder()
                .codigoCc(codigoLimpio)
                .nombreArea(request.getNombreArea().trim())
                .activo(true)
                .build());
        return CentroCostoResponse.builder()
                .id(cc.getId())
                .codigoCc(cc.getCodigoCc())
                .nombreArea(cc.getNombreArea())
                .activo(cc.getActivo())
                .build();
    }

    private DetalleRendicionCajaChica construirYValidarDetalleRendicion(
            DetalleRendicionRequest detReq, CajaChica cajaChica) {
        if (detReq.getProductoId() != null) {
            eventPublisher.publishEvent(
                    new ValidacionProductoSolicitadaEvent(detReq.getProductoId(), "finanzas.cajachica"));
        }

        CentroCosto centroCosto = null;
        if (detReq.getCentroCostoId() != null) {
            centroCosto = centroCostoRepository.findById(detReq.getCentroCostoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Centro de costo no encontrado con id: " + detReq.getCentroCostoId()));
        }

        BigDecimal subtotalDetalle = detReq.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detReq.getCantidad()));

        descontarPresupuestoCentroCosto(cajaChica, centroCosto, subtotalDetalle);

        return DetalleRendicionCajaChica.builder()
                .concepto(detReq.getConcepto())
                .cantidad(detReq.getCantidad())
                .precioUnitario(detReq.getPrecioUnitario())
                .subtotal(subtotalDetalle)
                .productoId(detReq.getProductoId())
                .centroCosto(centroCosto)
                .build();
    }

    private void descontarPresupuestoCentroCosto(
            CajaChica cajaChica, CentroCosto centroCosto, BigDecimal subtotalDetalle) {
        if (centroCosto == null) {
            return;
        }
        buscarAsignacionPorCentroCosto(cajaChica, centroCosto.getId()).ifPresent(asig -> {
            if (subtotalDetalle.compareTo(asig.getSaldoDisponible()) > 0) {
                throw new SolvenciaInsuficienteException(
                        "RN-FIN-06: El gasto para el Centro de Costo " + centroCosto.getCodigoCc()
                                + " (S/ " + subtotalDetalle + ") excede el saldo presupuestal disponible asignado (S/ "
                                + asig.getSaldoDisponible() + ")");
            }
            asig.descontarSaldo(subtotalDetalle);
        });
    }

    private void validarYDescontarSolvenciaCaja(CajaChica cajaChica, BigDecimal montoTotal) {
        if (montoTotal.compareTo(cajaChica.getSaldoDisponible()) > 0) {
            throw new SolvenciaInsuficienteException(
                    "RN-FIN-01: El monto total de la rendición (S/ " + montoTotal
                            + ") supera la solvencia/saldo disponible de Caja Chica (S/ "
                            + cajaChica.getSaldoDisponible() + ")");
        }
        cajaChica.setSaldoDisponible(cajaChica.getSaldoDisponible().subtract(montoTotal));
    }

    private void reintegrarSaldosPorCentroCosto(RendicionCajaChica rendicion, CajaChica caja) {
        if (rendicion.getDetalles() == null || caja.getAsignacionesPresupuesto() == null) {
            return;
        }
        for (DetalleRendicionCajaChica det : rendicion.getDetalles()) {
            if (det.getCentroCosto() != null) {
                buscarAsignacionPorCentroCosto(caja, det.getCentroCosto().getId())
                        .ifPresent(asig -> asig.reintegrarSaldo(det.getSubtotal()));
            }
        }
    }

    private Optional<AsignacionCajaCentroCosto> buscarAsignacionPorCentroCosto(CajaChica caja, Long centroCostoId) {
        if (caja.getAsignacionesPresupuesto() == null || centroCostoId == null) {
            return Optional.empty();
        }
        return caja.getAsignacionesPresupuesto().stream()
                .filter(a -> a.getCentroCosto() != null && a.getCentroCosto().getId().equals(centroCostoId))
                .findFirst();
    }

    private AsignacionCajaCentroCostoResponse mapToAsignacionResponse(AsignacionCajaCentroCosto a) {
        return AsignacionCajaCentroCostoResponse.builder()
                .cajaChicaId(a.getCajaChica().getId())
                .centroCostoId(a.getCentroCosto().getId())
                .codigoCc(a.getCentroCosto().getCodigoCc())
                .nombreArea(a.getCentroCosto().getNombreArea())
                .presupuestoAsignado(a.getPresupuestoAsignado())
                .saldoDisponible(a.getSaldoDisponible())
                .fechaAsignacion(a.getFechaAsignacion())
                .activo(a.getActivo())
                .build();
    }

    private CajaChica buscarCajaChicaOFallar(Long id) {
        return cajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja chica no encontrada con id: " + id));
    }

    private CajaChicaResponse mapToCajaChicaResponse(CajaChica caja) {
        return CajaChicaResponse.builder()
                .id(caja.getId())
                .codigo(caja.getCodigo())
                .responsable(caja.getResponsable())
                .saldoDisponible(caja.getSaldoDisponible())
                .limiteMaximo(caja.getLimiteMaximo())
                .build();
    }

    private RendicionCajaChicaResponse mapToRendicionResponse(RendicionCajaChica r) {
        List<DetalleRendicionResponse> detallesResp = r.getDetalles().stream()
                .map(d -> DetalleRendicionResponse.builder()
                        .id(d.getId())
                        .concepto(d.getConcepto())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .productoId(d.getProductoId())
                        .centroCostoId(d.getCentroCosto() != null ? d.getCentroCosto().getId() : null)
                        .centroCostoCodigo(d.getCentroCosto() != null ? d.getCentroCosto().getCodigoCc() : null)
                        .centroCostoNombre(d.getCentroCosto() != null ? d.getCentroCosto().getNombreArea() : null)
                        .build())
                .toList();

        return RendicionCajaChicaResponse.builder()
                .id(r.getId())
                .codigo(r.getCodigo())
                .responsable(r.getResponsable())
                .fecha(r.getFecha())
                .montoTotal(r.getMontoTotal())
                .estado(r.getEstado())
                .cajaChicaId(r.getCajaChica().getId())
                .detalles(detallesResp)
                .build();
    }
}
