package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.AbonoResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CrearCreditoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.RegistrarAbonoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.AbonoCuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.CuentaPorCobrar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.service.SesionCajaService;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;
import pe.edu.upeu.gdmerp.shared.event.ConsultaVentaPorTicketEvent;
import pe.edu.upeu.gdmerp.shared.event.PagoVentaRegistradoEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorCobrarServiceImpl implements CuentaPorCobrarService {

    private static final String NOMBRE_CATEGORIA_COBRO = "Cobros de Créditos y Fiados";
    private static final String PREFIJO_CREDITO_VENTA = "CRE-";

    private final CuentaPorCobrarRepository cuentaRepository;
    private final SesionCajaService sesionCajaService;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorCobrarResponse> listarTodos(EstadoCredito estado) {
        List<CuentaPorCobrar> lista = (estado != null)
                ? cuentaRepository.findByEstadoOrderByFechaEmisionDesc(estado)
                : cuentaRepository.findAllByOrderByFechaEmisionDesc();

        return lista.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorCobrarDetalleResponse obtenerPorId(Long id) {
        return mapToDetalleResponse(buscarCuentaOFallar(id));
    }

    @Override
    @Transactional
    public CuentaPorCobrarResponse registrarCredito(CrearCreditoRequest req) {
        String codigo = generarCodigoCredito(req.getCodigo());
        LocalDate fechaEmision = req.getFechaEmision() != null ? req.getFechaEmision() : LocalDate.now();
        LocalDate fechaVencimiento = req.getFechaVencimiento() != null
                ? req.getFechaVencimiento()
                : fechaEmision.plusDays(30);

        if (fechaVencimiento.isBefore(fechaEmision)) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la fecha de emisión.");
        }

        CuentaPorCobrar cuenta = CuentaPorCobrar.builder()
                .codigo(codigo)
                .clienteNombre(req.getClienteNombre().trim())
                .clienteDocumento(req.getClienteDocumento() != null ? req.getClienteDocumento().trim() : null)
                .montoTotal(req.getMontoTotal())
                .saldoPendiente(req.getMontoTotal())
                .fechaEmision(fechaEmision)
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoCredito.PENDIENTE)
                .build();

        CuentaPorCobrar guardada = cuentaRepository.save(cuenta);
        log.info("Crédito registrado exitosamente: {} para cliente {}", guardada.getCodigo(), guardada.getClienteNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public CuentaPorCobrarDetalleResponse registrarAbono(Long creditoId, RegistrarAbonoRequest req) {
        CuentaPorCobrar cuenta = buscarCuentaOFallar(creditoId);
        validarEstadoYMontoAbono(cuenta, req.getMonto());
        aplicarAmortizacion(cuenta, req.getMonto());

        Long sesionId = resolverSesionCajaParaAbono(req.getSesionCajaId());
        if (sesionId != null) {
            impactarMovimientoEnCaja(sesionId, cuenta, req);
        }

        cuenta.agregarAbono(construirAbono(cuenta, sesionId, req));
        CuentaPorCobrar guardada = cuentaRepository.save(cuenta);

        if (guardada.getEstado() == EstadoCredito.PAGADO_TOTAL) {
            notificarLiquidacionVentaAsociada(guardada);
        }

        log.info("Abono de S/ {} registrado con éxito en crédito {}. Saldo restante: S/ {}",
                req.getMonto(), guardada.getCodigo(), guardada.getSaldoPendiente());
        return mapToDetalleResponse(guardada);
    }

    private void notificarLiquidacionVentaAsociada(CuentaPorCobrar guardada) {
        if (guardada.getCodigo() == null || !guardada.getCodigo().startsWith(PREFIJO_CREDITO_VENTA)) {
            return;
        }
        String numeroTicket = guardada.getCodigo().substring(PREFIJO_CREDITO_VENTA.length()).trim();
        if (numeroTicket.isBlank()) {
            return;
        }

        TransaccionFinanciera trx = transaccionRepository.findFirstByNumeroComprobante(numeroTicket).orElse(null);
        if (trx != null && trx.getEstado() != EstadoFinanciero.PAGADO) {
            trx.setEstado(EstadoFinanciero.PAGADO);
            trx = transaccionRepository.save(trx);
        }

        Long transaccionId = trx != null ? trx.getId() : null;
        Long ventaId = trx != null ? trx.getVentaId() : null;
        if (ventaId == null) {
            ConsultaVentaPorTicketEvent consulta = new ConsultaVentaPorTicketEvent(numeroTicket);
            eventPublisher.publishEvent(consulta);
            ventaId = consulta.ventaIdEncontrado().get();
        }

        if (ventaId != null) {
            eventPublisher.publishEvent(new PagoVentaRegistradoEvent(
                    ventaId,
                    transaccionId,
                    guardada.getMontoTotal()
            ));
        }
    }

    private String generarCodigoCredito(String codigoSolicitado) {
        if (codigoSolicitado == null || codigoSolicitado.isBlank()) {
            long total = cuentaRepository.count() + 1;
            String codigo = String.format("CRE-%03d", total);
            if (cuentaRepository.existsByCodigo(codigo)) {
                return "CRE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"));
            }
            return codigo;
        }
        if (cuentaRepository.existsByCodigo(codigoSolicitado)) {
            throw new IllegalArgumentException("Ya existe una cuenta por cobrar con el código: " + codigoSolicitado);
        }
        return codigoSolicitado;
    }

    private void validarEstadoYMontoAbono(CuentaPorCobrar cuenta, BigDecimal monto) {
        if (cuenta.getEstado() == EstadoCredito.PAGADO_TOTAL) {
            throw new IllegalStateException("El crédito " + cuenta.getCodigo() + " ya se encuentra totalmente PAGADO.");
        }
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a abonar debe ser mayor a cero.");
        }
        if (monto.compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto a abonar (S/ %.2f) supera el saldo pendiente de la cuenta (S/ %.2f).",
                    monto, cuenta.getSaldoPendiente()
            ));
        }
    }

    private void aplicarAmortizacion(CuentaPorCobrar cuenta, BigDecimal monto) {
        BigDecimal nuevoSaldo = cuenta.getSaldoPendiente().subtract(monto);
        if (nuevoSaldo.compareTo(BigDecimal.ZERO) <= 0) {
            cuenta.setSaldoPendiente(BigDecimal.ZERO);
            cuenta.setEstado(EstadoCredito.PAGADO_TOTAL);
        } else {
            cuenta.setSaldoPendiente(nuevoSaldo);
            cuenta.setEstado(EstadoCredito.PAGADO_PARCIAL);
        }
    }

    private Long resolverSesionCajaParaAbono(Long sesionSolicitadaId) {
        if (sesionSolicitadaId != null) {
            return sesionSolicitadaId;
        }
        return sesionCajaService.buscarUltimaSesionActiva()
                .map(SesionCajaResponse::getId)
                .orElse(null);
    }

    private void impactarMovimientoEnCaja(Long sesionId, CuentaPorCobrar cuenta, RegistrarAbonoRequest req) {
        try {
            CategoriaFinanciera catCobro = resolverOCrearCategoriaCobro();
            MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                    .tipo(TipoFinanciero.INGRESO)
                    .metodoPago(req.getMetodoPago())
                    .monto(req.getMonto())
                    .descripcion(String.format("Cobro crédito %s - %s", cuenta.getCodigo(), cuenta.getClienteNombre()))
                    .referenciaExterna(cuenta.getCodigo())
                    .categoriaFinancieraId(catCobro.getId())
                    .build();

            sesionCajaService.registrarMovimiento(sesionId, movReq);
            log.info("Abono de S/ {} para crédito {} impactó la sesión de caja ID {}",
                    req.getMonto(), cuenta.getCodigo(), sesionId);
        } catch (Exception e) {
            log.warn("No se pudo registrar el movimiento en caja para la sesión {}: {}", sesionId, e.getMessage());
        }
    }

    private CategoriaFinanciera resolverOCrearCategoriaCobro() {
        return categoriaRepository.findByNombre(NOMBRE_CATEGORIA_COBRO)
                .or(() -> categoriaRepository.findByTipoDefault(TipoFinanciero.INGRESO).stream().findFirst())
                .orElseGet(() -> {
                    CategoriaFinanciera nueva = new CategoriaFinanciera();
                    nueva.setNombre(NOMBRE_CATEGORIA_COBRO);
                    nueva.setDescripcion("Ingresos por amortización y cobro de cuentas por cobrar");
                    nueva.setTipoDefault(TipoFinanciero.INGRESO);
                    return categoriaRepository.save(nueva);
                });
    }

    private AbonoCuentaPorCobrar construirAbono(CuentaPorCobrar cuenta, Long sesionId, RegistrarAbonoRequest req) {
        String nota = (req.getNota() != null && !req.getNota().isBlank())
                ? req.getNota().trim()
                : "Amortización de crédito " + cuenta.getCodigo();
        return AbonoCuentaPorCobrar.builder()
                .cuentaPorCobrar(cuenta)
                .sesionCajaId(sesionId)
                .monto(req.getMonto())
                .metodoPago(req.getMetodoPago())
                .fecha(LocalDateTime.now())
                .nota(nota)
                .build();
    }

    private CuentaPorCobrar buscarCuentaOFallar(Long id) {
        return cuentaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por cobrar no encontrada con ID: " + id));
    }

    private CuentaPorCobrarResponse mapToResponse(CuentaPorCobrar c) {
        BigDecimal montoTotal = c.getMontoTotal() != null ? c.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal saldoPendiente = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal montoPagado = montoTotal.subtract(saldoPendiente).max(BigDecimal.ZERO);

        double pct = montoTotal.compareTo(BigDecimal.ZERO) > 0
                ? montoPagado.multiply(BigDecimal.valueOf(100)).divide(montoTotal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        int abonosCount = c.getAbonos() != null ? c.getAbonos().size() : 0;

        return CuentaPorCobrarResponse.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .clienteNombre(c.getClienteNombre())
                .clienteDocumento(c.getClienteDocumento())
                .montoTotal(montoTotal)
                .saldoPendiente(saldoPendiente)
                .montoPagado(montoPagado)
                .porcentajePagado(pct)
                .fechaEmision(c.getFechaEmision())
                .fechaVencimiento(c.getFechaVencimiento())
                .estado(c.getEstado())
                .cantidadAbonos(abonosCount)
                .build();
    }

    private CuentaPorCobrarDetalleResponse mapToDetalleResponse(CuentaPorCobrar c) {
        CuentaPorCobrarResponse base = mapToResponse(c);
        List<AbonoResponse> abonosDto = (c.getAbonos() != null)
                ? c.getAbonos().stream().map(a -> AbonoResponse.builder()
                        .id(a.getId())
                        .cuentaPorCobrarId(c.getId())
                        .sesionCajaId(a.getSesionCajaId())
                        .monto(a.getMonto())
                        .metodoPago(a.getMetodoPago())
                        .fecha(a.getFecha())
                        .nota(a.getNota())
                        .build()).toList()
                : List.of();

        return CuentaPorCobrarDetalleResponse.builder()
                .id(base.getId())
                .codigo(base.getCodigo())
                .clienteNombre(base.getClienteNombre())
                .clienteDocumento(base.getClienteDocumento())
                .montoTotal(base.getMontoTotal())
                .saldoPendiente(base.getSaldoPendiente())
                .montoPagado(base.getMontoPagado())
                .porcentajePagado(base.getPorcentajePagado())
                .fechaEmision(base.getFechaEmision())
                .fechaVencimiento(base.getFechaVencimiento())
                .estado(base.getEstado())
                .abonos(abonosDto)
                .build();
    }
}
