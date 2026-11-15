package pe.edu.upeu.gdmerp.finanzas.cajachica.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.CentroCostoResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.DetalleRendicionRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaRequest;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionCajaChicaResponse;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.RendicionResumenDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.dto.ReporteRendicionesAgregadoDto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.CentroCosto;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.DetalleRendicionCajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.RendicionCajaChica;
import pe.edu.upeu.gdmerp.finanzas.cajachica.mapper.CajaChicaMapper;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CajaChicaRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.CentroCostoRepository;
import pe.edu.upeu.gdmerp.finanzas.cajachica.repository.RendicionCajaChicaRepository;
import pe.edu.upeu.gdmerp.shared.event.ValidacionProductoSolicitadaEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class RendicionCajaChicaServiceImpl implements RendicionCajaChicaService {

    private final CajaChicaRepository cajaChicaRepository;
    private final RendicionCajaChicaRepository rendicionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CentroCostoRepository centroCostoRepository;
    private final CajaChicaMapper cajaChicaMapper;

    public RendicionCajaChicaServiceImpl(
            CajaChicaRepository cajaChicaRepository,
            RendicionCajaChicaRepository rendicionRepository,
            ApplicationEventPublisher eventPublisher,
            CentroCostoRepository centroCostoRepository,
            CajaChicaMapper cajaChicaMapper) {
        this.cajaChicaRepository = cajaChicaRepository;
        this.rendicionRepository = rendicionRepository;
        this.eventPublisher = eventPublisher;
        this.centroCostoRepository = centroCostoRepository;
        this.cajaChicaMapper = cajaChicaMapper;
    }

    @Override
    @Transactional
    public CajaChicaResponse crearCajaChica(
            String codigo, String responsable, BigDecimal limiteMaximo, BigDecimal saldoInicial, Long centroCostoId) {
        if (cajaChicaRepository.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe una caja chica con el código: " + codigo);
        }
        validarCentroCostoSiPresente(centroCostoId);

        CajaChica caja = CajaChica.builder()
                .codigo(codigo)
                .responsable(responsable)
                .limiteMaximo(limiteMaximo)
                .saldoDisponible(saldoInicial)
                .centroCostoId(centroCostoId)
                .build();

        return cajaChicaMapper.toCajaChicaResponse(cajaChicaRepository.save(caja));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CajaChicaResponse> listarCajasChicas() {
        return cajaChicaMapper.toCajaChicaResponseList(cajaChicaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CajaChicaResponse obtenerCajaChica(Long id) {
        CajaChica caja = cajaChicaRepository.findWithCustodiosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja chica no encontrada con id: " + id));
        return cajaChicaMapper.toCajaChicaResponse(caja);
    }

    @Override
    @Transactional
    public RendicionCajaChicaResponse anularRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findWithDetallesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));

        if (rendicion.getEstado() == EstadoRendicion.ANULADO) {
            throw new IllegalStateException("RN-FIN-02: La rendición con código " + rendicion.getCodigo()
                    + " ya se encuentra en estado ANULADO y no se puede anular nuevamente.");
        }

        rendicion.setEstado(EstadoRendicion.ANULADO);
        CajaChica caja = rendicion.getCajaChica();
        caja.setSaldoDisponible(caja.getSaldoDisponible().add(rendicion.getMontoTotal()));
        cajaChicaRepository.save(caja);

        return cajaChicaMapper.toRendicionResponse(rendicionRepository.save(rendicion));
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
        return cajaChicaMapper.toRendicionResponse(rendicionRepository.save(rendicion));
    }

    @Override
    @Transactional(readOnly = true)
    public RendicionCajaChicaResponse obtenerRendicion(Long id) {
        RendicionCajaChica rendicion = rendicionRepository.findWithDetallesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendición no encontrada con id: " + id));
        return cajaChicaMapper.toRendicionResponse(rendicion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RendicionCajaChicaResponse> listarRendicionesPorCaja(Long cajaChicaId) {
        buscarCajaChicaOFallar(cajaChicaId);
        return cajaChicaMapper.toRendicionList(rendicionRepository.findByCajaChicaId(cajaChicaId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RendicionCajaChicaResponse> listarPaginado(Pageable pageable) {
        return rendicionRepository.findAll(pageable).map(cajaChicaMapper::toRendicionListResponse);
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
        ).map(cajaChicaMapper::toRendicionListResponse);
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
    @Transactional(readOnly = true)
    public List<CentroCostoResponse> listarCentrosCosto() {
        return cajaChicaMapper.toCentroCostoResponseList(
                centroCostoRepository.findByActivoTrueOrderByCodigoCcAsc());
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
        return cajaChicaMapper.toCentroCostoResponse(cc);
    }

    private DetalleRendicionCajaChica construirYValidarDetalleRendicion(
            DetalleRendicionRequest detReq, CajaChica cajaChica) {
        if (detReq.getProductoId() != null) {
            eventPublisher.publishEvent(
                    new ValidacionProductoSolicitadaEvent(detReq.getProductoId(), "finanzas.cajachica"));
        }

        CentroCosto centroCosto = resolverCentroCostoDetalle(detReq.getCentroCostoId(), cajaChica);
        BigDecimal subtotalDetalle = detReq.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(detReq.getCantidad()));

        return DetalleRendicionCajaChica.builder()
                .concepto(detReq.getConcepto())
                .cantidad(detReq.getCantidad())
                .precioUnitario(detReq.getPrecioUnitario())
                .subtotal(subtotalDetalle)
                .productoId(detReq.getProductoId())
                .centroCosto(centroCosto)
                .build();
    }

    private CentroCosto resolverCentroCostoDetalle(Long detalleCentroCostoId, CajaChica cajaChica) {
        Long centroCostoId = detalleCentroCostoId != null ? detalleCentroCostoId : cajaChica.getCentroCostoId();
        if (centroCostoId == null) {
            return null;
        }
        return centroCostoRepository.findById(centroCostoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Centro de costo no encontrado con id: " + centroCostoId));
    }

    private void validarCentroCostoSiPresente(Long centroCostoId) {
        if (centroCostoId != null && !centroCostoRepository.existsById(centroCostoId)) {
            throw new ResourceNotFoundException("Centro de costo no encontrado con id: " + centroCostoId);
        }
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

    private CajaChica buscarCajaChicaOFallar(Long id) {
        return cajaChicaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Caja chica no encontrada con id: " + id));
    }
}
