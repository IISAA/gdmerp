package pe.edu.upeu.gdmerp.finanzas.sesioncaja.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.SolvenciaInsuficienteException;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.AperturaSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DesgloseEfectivoItemDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DenominacionMoneda;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivo;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.DetalleArqueoEfectivoId;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.mapper.FinanzasMappingHelper;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.mapper.SesionCajaMapper;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.DenominacionMonedaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.DetalleArqueoEfectivoRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.MovimientoCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SesionCajaServiceImpl implements SesionCajaService {

    private static final DateTimeFormatter CODIGO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SesionCajaRepository sesionRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final DenominacionMonedaRepository denominacionRepository;
    private final DetalleArqueoEfectivoRepository detalleArqueoRepository;
    private final SesionCajaMapper sesionMapper;

    public SesionCajaServiceImpl(
            SesionCajaRepository sesionRepository,
            MovimientoCajaRepository movimientoRepository,
            CategoriaFinancieraRepository categoriaRepository,
            DenominacionMonedaRepository denominacionRepository,
            DetalleArqueoEfectivoRepository detalleArqueoRepository,
            SesionCajaMapper sesionMapper) {
        this.sesionRepository = sesionRepository;
        this.movimientoRepository = movimientoRepository;
        this.categoriaRepository = categoriaRepository;
        this.denominacionRepository = denominacionRepository;
        this.detalleArqueoRepository = detalleArqueoRepository;
        this.sesionMapper = sesionMapper;
    }

    @Override
    @Transactional
    public SesionCajaResponse abrirSesion(AperturaSesionRequest request) {
        if (sesionRepository.existsByUsuarioIdAndEstado(request.getUsuarioId(), EstadoCaja.ABIERTA)) {
            throw new IllegalStateException("RN-FIN-02: El usuario ya posee un turno de caja abierto. Debe cerrarlo antes de iniciar otro.");
        }

        String codigo = resolverCodigoSesion(request.getCodigoSesion());
        if (sesionRepository.existsByCodigoSesion(codigo)) {
            throw new IllegalArgumentException("Ya existe una sesión con el código: " + codigo);
        }

        String rol = (request.getCajeroRol() != null && !request.getCajeroRol().isBlank())
                ? request.getCajeroRol()
                : "CAJERO";

        SesionCaja sesion = SesionCaja.builder()
                .codigoSesion(codigo)
                .usuarioId(request.getUsuarioId())
                .cajero(request.getCajero())
                .cajeroRol(rol)
                .fechaApertura(LocalDateTime.now())
                .saldoInicial(request.getSaldoInicial())
                .totalIngresosEfectivo(BigDecimal.ZERO)
                .totalEgresosEfectivo(BigDecimal.ZERO)
                .saldoEsperadoEfectivo(request.getSaldoInicial())
                .saldoRealEfectivo(null)
                .diferencia(BigDecimal.ZERO)
                .estado(EstadoCaja.ABIERTA)
                .build();

        return sesionMapper.toSummaryResponse(sesionRepository.save(sesion));
    }

    @Override
    @Transactional
    public SesionCajaResponse cerrarSesion(Long id, CierreSesionRequest request) {
        SesionCaja sesion = sesionRepository.findWithMovimientosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con id: " + id));

        if (sesion.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalStateException(
                    "RN-FIN-02: La sesión " + sesion.getCodigoSesion() + " ya se encuentra CERRADA.");
        }

        BigDecimal saldoReal = (request.getDesglose() != null && !request.getDesglose().isEmpty())
                ? registrarArqueoFisico(sesion, request.getDesglose())
                : request.getSaldoRealEfectivo();

        sesion.cerrarSesion(saldoReal, request.getObservaciones());
        SesionCaja guardada = sesionRepository.save(sesion);
        return mapSingleWithCount(guardada);
    }

    @Override
    @Transactional
    public MovimientoCajaResponse registrarMovimiento(Long sesionId, MovimientoCajaRequest request) {
        SesionCaja sesion = sesionRepository.findWithMovimientosById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con id: " + sesionId));

        if (sesion.getEstado() != EstadoCaja.ABIERTA) {
            throw new IllegalStateException("RN-FIN-02: La sesión " + sesion.getCodigoSesion()
                    + " no está ABIERTA. No se pueden registrar movimientos.");
        }

        CategoriaFinanciera categoria = categoriaRepository.findById(request.getCategoriaFinancieraId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría financiera no encontrada con id: " + request.getCategoriaFinancieraId()));

        validarSolvenciaEfectivo(sesion, request);

        MovimientoCaja movimiento = MovimientoCaja.builder()
                .fechaHora(LocalDateTime.now())
                .tipo(request.getTipo())
                .metodoPago(request.getMetodoPago())
                .monto(request.getMonto())
                .descripcion(request.getDescripcion())
                .referenciaExterna(request.getReferenciaExterna())
                .ventaId(request.getVentaId())
                .compraId(request.getCompraId())
                .categoriaFinanciera(categoria)
                .sesionCaja(sesion)
                .build();

        sesion.agregarMovimiento(movimiento);
        sesionRepository.save(sesion);
        return sesionMapper.toMovimientoResponse(movimientoRepository.save(movimiento));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionCajaResponse> listarSesiones() {
        List<SesionCaja> sesiones = sesionRepository.findAllByOrderByFechaAperturaDesc();
        return sesionMapper.toResponseList(
                sesiones, FinanzasMappingHelper.toCountMap(sesionRepository.contarMovimientosPorSesion()));
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaDetalleResponse obtenerDetalle(Long id) {
        SesionCaja sesion = sesionRepository.findWithMovimientosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con id: " + id));

        List<DesgloseEfectivoItemDto> arqueo = detalleArqueoRepository.findBySesionCajaId(id).stream()
                .map(sesionMapper::toDesgloseResponse)
                .toList();

        SesionCajaDetalleResponse detalle = sesionMapper.toListDetalleResponse(sesion);
        detalle.setCajeroRol(sesion.getCajeroRol() != null ? sesion.getCajeroRol() : "CAJERO");
        detalle.setMovimientos(sesionMapper.toMovimientoResponseList(sesion.getMovimientos()));
        detalle.setDetallesArqueo(arqueo);
        return detalle;
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerSesionActivaUsuario(Long usuarioId) {
        SesionCaja sesion = sesionRepository
                .findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe sesión abierta para el usuario id: " + usuarioId));
        return mapSingleWithCount(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SesionCajaResponse> buscarUltimaSesionActiva() {
        return sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA)
                .map(this::mapSingleWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerUltimaSesionActiva() {
        return buscarUltimaSesionActiva()
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna sesión de caja abierta actualmente."));
    }

    @Override
    @Transactional(readOnly = true)
    public CierreCajaReporteDto generarDatosReporte(Long id) {
        SesionCaja sesion = sesionRepository.findWithMovimientosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con id: " + id));

        List<MovimientoCajaResponse> movs = sesionMapper.toMovimientoResponseList(sesion.getMovimientos());
        List<MetodoPagoResumenDto> resumenMetodos = construirResumenMetodosPago(sesion.getMovimientos());

        return CierreCajaReporteDto.builder()
                .fechaGeneracion(LocalDateTime.now())
                .sesion(sesionMapper.toResponse(sesion, Map.of(sesion.getId(), movs.size())))
                .movimientos(movs)
                .resumenPorMetodo(resumenMetodos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DenominacionMonedaResponse> listarDenominaciones() {
        return sesionMapper.toDenominacionResponseList(
                denominacionRepository.findByActivoTrueOrderByValorFacialDesc());
    }

    private String resolverCodigoSesion(String codigoSolicitado) {
        if (codigoSolicitado != null && !codigoSolicitado.isBlank()) {
            return codigoSolicitado;
        }
        return "SES-" + LocalDateTime.now().format(CODIGO_FORMATTER);
    }

    private BigDecimal registrarArqueoFisico(SesionCaja sesion, List<DesgloseEfectivoItemDto> desglose) {
        BigDecimal totalDesglose = BigDecimal.ZERO;
        sesion.getDetallesArqueo().clear();

        for (DesgloseEfectivoItemDto item : desglose) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                continue;
            }
            DenominacionMoneda denom = denominacionRepository.findById(item.getDenominacionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Denominación no encontrada con id: " + item.getDenominacionId()));

            BigDecimal subtotal = denom.getValorFacial().multiply(BigDecimal.valueOf(item.getCantidad()));
            totalDesglose = totalDesglose.add(subtotal);

            DetalleArqueoEfectivo detalle = DetalleArqueoEfectivo.builder()
                    .id(new DetalleArqueoEfectivoId(sesion.getId(), denom.getId()))
                    .sesionCaja(sesion)
                    .denominacionMoneda(denom)
                    .cantidad(item.getCantidad())
                    .subtotal(subtotal)
                    .build();
            sesion.agregarDetalleArqueo(detalle);
        }
        return totalDesglose;
    }

    private void validarSolvenciaEfectivo(SesionCaja sesion, MovimientoCajaRequest request) {
        if (request.getTipo() != TipoFinanciero.EGRESO || request.getMetodoPago() != MetodoPago.EFECTIVO) {
            return;
        }
        BigDecimal saldoDisponible = sesion.getSaldoEsperadoEfectivo() != null
                ? sesion.getSaldoEsperadoEfectivo()
                : BigDecimal.ZERO;
        if (request.getMonto().compareTo(saldoDisponible) > 0) {
            throw new SolvenciaInsuficienteException(
                    "RN-FIN-01: El monto del egreso en efectivo (S/ " + request.getMonto() +
                            ") excede el saldo en efectivo disponible en gaveta (S/ " + saldoDisponible + ")");
        }
    }

    private List<MetodoPagoResumenDto> construirResumenMetodosPago(List<MovimientoCaja> movimientos) {
        Map<MetodoPago, BigDecimal[]> acumuladores = new EnumMap<>(MetodoPago.class);
        for (MetodoPago metodo : MetodoPago.values()) {
            acumuladores.put(metodo, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (MovimientoCaja mov : movimientos) {
            BigDecimal[] par = acumuladores.get(mov.getMetodoPago());
            if (par == null || mov.getMonto() == null) {
                continue;
            }
            if (mov.getTipo() == TipoFinanciero.INGRESO) {
                par[0] = par[0].add(mov.getMonto());
            } else if (mov.getTipo() == TipoFinanciero.EGRESO) {
                par[1] = par[1].add(mov.getMonto());
            }
        }

        List<MetodoPagoResumenDto> resumenMetodos = new ArrayList<>();
        for (Map.Entry<MetodoPago, BigDecimal[]> entry : acumuladores.entrySet()) {
            BigDecimal ing = entry.getValue()[0];
            BigDecimal egr = entry.getValue()[1];
            resumenMetodos.add(MetodoPagoResumenDto.builder()
                    .metodoPago(entry.getKey())
                    .totalIngresos(ing)
                    .totalEgresos(egr)
                    .neto(ing.subtract(egr))
                    .build());
        }
        return resumenMetodos;
    }

    private SesionCajaResponse mapSingleWithCount(SesionCaja sesion) {
        int count = Math.toIntExact(movimientoRepository.countBySesionCajaId(sesion.getId()));
        SesionCajaResponse response = sesionMapper.toResponse(sesion, Map.of(sesion.getId(), count));
        if (response.getCajeroRol() == null) {
            response.setCajeroRol("CAJERO");
        }
        return response;
    }
}
