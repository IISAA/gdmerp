package pe.edu.upeu.gdmerp.finanzas.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CrearCreditoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.repository.CuentaPorCobrarRepository;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.service.CuentaPorCobrarService;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.CuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.repository.CuentaPorPagarRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.service.SesionCajaService;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanzasApiImpl implements FinanzasApi {

    private static final String CATEGORIA_VENTAS = "Ventas POS y Comercialización";
    private static final String CATEGORIA_DEVOLUCIONES = "Devoluciones y Anulaciones POS";
    private static final String CATEGORIA_COMPRAS = "Insumos y Envases de Producción";

    private final SesionCajaRepository sesionRepository;
    private final SesionCajaService sesionService;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final CuentaPorCobrarService cuentaPorCobrarService;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;
    private final CuentaPorPagarRepository cuentaPorPagarRepository;

    @Override
    @Transactional
    public Long registrarCobroVenta(Long usuarioId, BigDecimal monto, MetodoPago metodoPago, String referenciaVenta, Long categoriaId) {
        SesionCaja sesion = sesionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .orElseThrow(() -> new IllegalStateException(
                        "No se puede registrar el cobro de la venta " + referenciaVenta +
                                " porque el usuario " + usuarioId + " no tiene un turno de caja ABIERTO."
                ));

        Long catId = categoriaId != null
                ? categoriaId
                : obtenerOCrearCategoria(TipoFinanciero.INGRESO, CATEGORIA_VENTAS).getId();

        MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                .tipo(TipoFinanciero.INGRESO)
                .metodoPago(metodoPago)
                .monto(monto)
                .descripcion("Cobro de Venta " + (referenciaVenta != null ? referenciaVenta : ""))
                .referenciaExterna(referenciaVenta)
                .categoriaFinancieraId(catId)
                .build();

        return sesionService.registrarMovimiento(sesion.getId(), movReq).getId();
    }

    @Override
    @Transactional
    public Long registrarIngresoVentaPos(
            Long ventaId,
            BigDecimal monto,
            MetodoPago metodoPago,
            String numeroTicket,
            String clienteNombre,
            Long sesionCajaId,
            String cajeroId) {
        if (!esMontoPositivo(monto)) {
            return null;
        }
        CategoriaFinanciera catIngreso = obtenerOCrearCategoria(TipoFinanciero.INGRESO, CATEGORIA_VENTAS);
        MetodoPago mp = metodoPago != null ? metodoPago : MetodoPago.EFECTIVO;
        SesionCaja sesionDestino = resolverSesionAbiertaParaVenta(sesionCajaId, cajeroId);

        if (mp == MetodoPago.EFECTIVO && sesionDestino == null) {
            throw new BusinessConflictException(
                    "No existe un turno de caja abierto para registrar cobros en efectivo (Ticket " + numeroTicket + ")."
            );
        }

        Long movimientoId = registrarMovimientoVentaEnCaja(sesionDestino, ventaId, monto, mp, numeroTicket, clienteNombre, catIngreso.getId());
        guardarTransaccionContable(
                generarCodigoUnico("TRX-VTA", numeroTicket),
                truncar("Venta POS " + numeroTicket + " - " + resolverNombreCliente(clienteNombre), 195),
                monto,
                LocalDate.now(),
                TipoFinanciero.INGRESO,
                EstadoFinanciero.PAGADO,
                "TICKET_POS",
                numeroTicket,
                ventaId,
                null,
                catIngreso
        );

        log.info("Ingreso financiero por Venta POS {} (S/ {}) asentado en Sesión #{} y Finanzas.",
                numeroTicket, monto, (sesionDestino != null ? sesionDestino.getId() : "NINGUNA"));
        return movimientoId;
    }

    @Override
    @Transactional
    public Long registrarVentaACredito(
            Long ventaId,
            Long clienteId,
            String numeroTicket,
            String clienteNombre,
            String clienteDocumento,
            BigDecimal montoTotal,
            Integer diasCredito) {
        if (!esMontoPositivo(montoTotal)) {
            return null;
        }
        LocalDate hoy = LocalDate.now();
        int dias = (diasCredito != null && diasCredito > 0) ? diasCredito : 30;
        String codigoCredito = truncar("CRE-" + numeroTicket, 48);

        CrearCreditoRequest creditoReq = CrearCreditoRequest.builder()
                .codigo(codigoCredito)
                .clienteId(clienteId)
                .ventaId(ventaId)
                .clienteNombre(resolverNombreCliente(clienteNombre))
                .clienteDocumento(clienteDocumento)
                .montoTotal(montoTotal)
                .fechaEmision(hoy)
                .fechaVencimiento(hoy.plusDays(dias))
                .nota("Crédito generado automáticamente por Venta POS " + numeroTicket)
                .build();

        CuentaPorCobrarResponse cuenta = cuentaPorCobrarService.registrarCredito(creditoReq);
        CategoriaFinanciera catIngreso = obtenerOCrearCategoria(TipoFinanciero.INGRESO, CATEGORIA_VENTAS);

        guardarTransaccionContable(
                generarCodigoUnico("TRX-CRE", numeroTicket),
                truncar("Venta al Crédito " + numeroTicket + " - " + creditoReq.getClienteNombre(), 195),
                montoTotal,
                hoy,
                TipoFinanciero.INGRESO,
                EstadoFinanciero.PENDIENTE,
                "CREDITO_POS",
                numeroTicket,
                ventaId,
                null,
                catIngreso
        );
        return cuenta.getId();
    }

    @Override
    @Transactional
    public void revertirVentaPos(Long ventaId, String numeroTicket, BigDecimal monto, MetodoPago metodoPago, boolean eraCredito, Long sesionCajaId) {
        anularTransaccionPorVenta(ventaId, numeroTicket);

        if (eraCredito) {
            revertirCuentaPorCobrarVenta(ventaId, numeroTicket);
            return;
        }

        if (esMontoPositivo(monto)) {
            registrarExtornoVentaPos(ventaId, numeroTicket, monto, metodoPago, sesionCajaId);
        }
    }

    @Override
    @Transactional
    public void registrarFacturaCompraPendiente(
            Long compraId,
            Long proveedorId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoTotal,
            LocalDate fechaEmision) {
        if (!esMontoPositivo(montoTotal)) {
            return;
        }
        LocalDate emision = fechaEmision != null ? fechaEmision : LocalDate.now();
        String proveedor = (razonSocialProveedor != null && !razonSocialProveedor.isBlank())
                ? razonSocialProveedor.trim()
                : "Proveedor General";
        CategoriaFinanciera catCompra = obtenerOCrearCategoria(TipoFinanciero.EGRESO, CATEGORIA_COMPRAS);

        registrarCuentaPorPagarSiNoExiste(compraId, proveedorId, numeroComprobante, proveedor, montoTotal, emision);

        guardarTransaccionContable(
                generarCodigoUnico("TRX-COM", numeroComprobante),
                truncar("Factura de Compra " + numeroComprobante + " - " + proveedor, 195),
                montoTotal,
                emision,
                TipoFinanciero.EGRESO,
                EstadoFinanciero.PENDIENTE,
                "FACTURA",
                numeroComprobante,
                null,
                compraId,
                catCompra
        );
    }

    @Override
    @Transactional
    public void confirmarRecepcionCompra(Long compraId, String numeroComprobante) {
        buscarCuentaPorPagar(compraId, numeroComprobante).ifPresentOrElse(
                cuenta -> {
                    cuenta.marcarRecepcionConfirmada();
                    cuentaPorPagarRepository.save(cuenta);
                    log.info("[Finanzas] Recepción confirmada en CuentaPorPagar {} (compraId={}).",
                            cuenta.getCodigo(), compraId);
                },
                () -> log.debug("[Finanzas] RecepcionCompraEvent recibido sin CuentaPorPagar previa (compraId={}, comprobante={}).",
                        compraId, numeroComprobante)
        );
    }

    @Override
    @Transactional
    public void registrarPagoProveedorCompra(
            Long compraId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            boolean liquidadaTotalmente) {
        if (!esMontoPositivo(montoPagado)) {
            return;
        }
        CategoriaFinanciera catCompra = obtenerOCrearCategoria(TipoFinanciero.EGRESO, CATEGORIA_COMPRAS);
        MetodoPago mp = metodoPago != null ? metodoPago : MetodoPago.TRANSFERENCIA;

        impactarEgresoCompraEnCajaActiva(compraId, numeroComprobante, razonSocialProveedor, montoPagado, mp, catCompra);
        amortizarCuentaPorPagarCompra(compraId, numeroComprobante, montoPagado, mp);
        actualizarTransaccionCompraPorPago(compraId, numeroComprobante, razonSocialProveedor, montoPagado, liquidadaTotalmente, catCompra);
    }

    @Override
    @Transactional
    public void anularFacturaCompra(Long compraId, String numeroComprobante) {
        if (compraId == null && numeroComprobante == null) {
            return;
        }
        buscarCuentaPorPagar(compraId, numeroComprobante).ifPresent(cuenta -> {
            if (cuenta.getEstado() != EstadoCuentaPorPagar.ANULADO) {
                cuenta.anular();
                cuentaPorPagarRepository.save(cuenta);
            }
        });
        buscarTransaccionCompra(compraId, numeroComprobante).ifPresent(trx -> {
            trx.setEstado(EstadoFinanciero.ANULADO);
            transaccionRepository.save(trx);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneSesionAbierta(Long usuarioId) {
        return sesionRepository.existsByUsuarioIdAndEstado(usuarioId, EstadoCaja.ABIERTA);
    }

    @Override
    @Transactional(readOnly = true)
    public Long obtenerIdSesionActiva(Long usuarioId) {
        return sesionRepository.findFirstByUsuarioIdAndEstadoOrderByFechaAperturaDesc(usuarioId, EstadoCaja.ABIERTA)
                .map(SesionCaja::getId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene una sesión de caja activa."));
    }

    private SesionCaja resolverSesionAbiertaParaVenta(Long sesionCajaId, String cajeroId) {
        if (sesionCajaId != null) {
            SesionCaja sesion = sesionRepository.findById(sesionCajaId).orElse(null);
            if (sesion != null && sesion.getEstado() != EstadoCaja.ABIERTA) {
                throw new BusinessConflictException("La sesión de caja #" + sesionCajaId + " no se encuentra ABIERTA.");
            }
            if (sesion != null) {
                return sesion;
            }
        }
        if (cajeroId != null && !cajeroId.isBlank()) {
            Optional<SesionCaja> porCajero = sesionRepository
                    .findByCajeroAndEstadoOrderByFechaAperturaDesc(cajeroId, EstadoCaja.ABIERTA)
                    .stream()
                    .findFirst();
            if (porCajero.isPresent()) {
                return porCajero.get();
            }
        }
        return sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA).orElse(null);
    }

    private Long registrarMovimientoVentaEnCaja(
            SesionCaja sesionDestino,
            Long ventaId,
            BigDecimal monto,
            MetodoPago metodoPago,
            String numeroTicket,
            String clienteNombre,
            Long categoriaId) {
        if (sesionDestino == null) {
            return null;
        }
        MovimientoCajaRequest movReq = MovimientoCajaRequest.builder()
                .tipo(TipoFinanciero.INGRESO)
                .metodoPago(metodoPago)
                .monto(monto)
                .descripcion(truncar("Venta POS " + numeroTicket + " - " + resolverNombreCliente(clienteNombre), 200))
                .referenciaExterna(numeroTicket)
                .ventaId(ventaId)
                .categoriaFinancieraId(categoriaId)
                .build();
        MovimientoCajaResponse resp = sesionService.registrarMovimiento(sesionDestino.getId(), movReq);
        return resp.getId();
    }

    private void anularTransaccionPorVenta(Long ventaId, String numeroTicket) {
        Optional<TransaccionFinanciera> trxOpt = (ventaId != null)
                ? transaccionRepository.findFirstByVentaId(ventaId)
                : Optional.empty();
        if (trxOpt.isEmpty() && numeroTicket != null) {
            trxOpt = transaccionRepository.findFirstByNumeroComprobante(numeroTicket);
        }
        trxOpt.ifPresent(trx -> {
            trx.setEstado(EstadoFinanciero.ANULADO);
            transaccionRepository.save(trx);
        });
    }

    private void revertirCuentaPorCobrarVenta(Long ventaId, String numeroTicket) {
        if (ventaId != null) {
            var porVenta = cuentaPorCobrarRepository.findFirstByVentaId(ventaId);
            if (porVenta.isPresent()) {
                cuentaPorCobrarRepository.delete(porVenta.get());
                return;
            }
        }
        if (numeroTicket != null) {
            String codigoCredito = truncar("CRE-" + numeroTicket, 48);
            cuentaPorCobrarRepository.findByCodigo(codigoCredito).ifPresent(cuentaPorCobrarRepository::delete);
        }
    }

    private void registrarExtornoVentaPos(Long ventaId, String numeroTicket, BigDecimal monto, MetodoPago metodoPago, Long sesionCajaId) {
        SesionCaja sesionDestino = resolverSesionAbiertaPreferida(sesionCajaId);
        MetodoPago mp = metodoPago != null ? metodoPago : MetodoPago.EFECTIVO;
        CategoriaFinanciera catEgreso = obtenerOCrearCategoria(TipoFinanciero.EGRESO, CATEGORIA_DEVOLUCIONES);

        if (sesionDestino != null && tieneSolvenciaParaExtorno(sesionDestino, mp, monto)) {
            String desc = (sesionCajaId != null && !sesionDestino.getId().equals(sesionCajaId))
                    ? "Extorno por anulación de Venta " + numeroTicket + " (Sesión #" + sesionCajaId + " ya cerrada)"
                    : "Extorno por anulación de Venta " + numeroTicket;
            sesionService.registrarMovimiento(sesionDestino.getId(), MovimientoCajaRequest.builder()
                    .tipo(TipoFinanciero.EGRESO)
                    .metodoPago(mp)
                    .monto(monto)
                    .descripcion(truncar(desc, 200))
                    .referenciaExterna(numeroTicket)
                    .ventaId(ventaId)
                    .categoriaFinancieraId(catEgreso.getId())
                    .build());
            return;
        }

        guardarTransaccionContable(
                generarCodigoUnico("EXT-VTA", numeroTicket),
                truncar("Extorno Venta " + numeroTicket + (sesionDestino == null ? " (Sin caja abierta)" : " (Sin solvencia en turno)"), 195),
                monto,
                LocalDate.now(),
                TipoFinanciero.EGRESO,
                EstadoFinanciero.PAGADO,
                "EXTORNO_POS",
                numeroTicket,
                ventaId,
                null,
                catEgreso
        );
    }

    private SesionCaja resolverSesionAbiertaPreferida(Long sesionCajaId) {
        if (sesionCajaId != null) {
            SesionCaja sc = sesionRepository.findById(sesionCajaId).orElse(null);
            if (sc != null && sc.getEstado() == EstadoCaja.ABIERTA) {
                return sc;
            }
        }
        return sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA).orElse(null);
    }

    private boolean tieneSolvenciaParaExtorno(SesionCaja sesion, MetodoPago mp, BigDecimal monto) {
        return mp != MetodoPago.EFECTIVO
                || (sesion.getSaldoEsperadoEfectivo() != null && sesion.getSaldoEsperadoEfectivo().compareTo(monto) >= 0);
    }

    private void registrarCuentaPorPagarSiNoExiste(
            Long compraId,
            Long proveedorId,
            String numeroComprobante,
            String proveedor,
            BigDecimal montoTotal,
            LocalDate emision) {
        if (buscarCuentaPorPagar(compraId, numeroComprobante).isPresent()) {
            return;
        }
        String codigoCxp = resolverCodigoCuentaPorPagar(numeroComprobante, compraId);
        CuentaPorPagar cxp = CuentaPorPagar.builder()
                .codigo(codigoCxp)
                .proveedorId(proveedorId)
                .compraId(compraId)
                .proveedorRazonSocial(truncar(proveedor, 195))
                .numeroComprobante(numeroComprobante)
                .descripcion(truncar("Obligación por compra " + (numeroComprobante != null ? numeroComprobante : ""), 250))
                .montoTotal(montoTotal)
                .saldoPendiente(montoTotal)
                .fechaEmision(emision)
                .fechaVencimiento(emision.plusDays(30))
                .estado(EstadoCuentaPorPagar.PENDIENTE)
                .recepcionConfirmada(false)
                .build();
        cuentaPorPagarRepository.save(cxp);
    }

    private String resolverCodigoCuentaPorPagar(String numeroComprobante, Long compraId) {
        String base = (numeroComprobante != null && !numeroComprobante.isBlank())
                ? "CXP-" + numeroComprobante.trim()
                : (compraId != null ? "CXP-CMP-" + compraId : "CXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        String candidato = truncar(base, 48);
        if (cuentaPorPagarRepository.findByCodigo(candidato).isPresent()) {
            return generarCodigoUnico("CXP", numeroComprobante);
        }
        return candidato;
    }

    private void impactarEgresoCompraEnCajaActiva(
            Long compraId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            MetodoPago metodoPago,
            CategoriaFinanciera catCompra) {
        sesionRepository.findFirstByEstadoOrderByFechaAperturaDesc(EstadoCaja.ABIERTA)
                .ifPresent(sesionActiva -> sesionService.registrarMovimiento(
                        sesionActiva.getId(),
                        MovimientoCajaRequest.builder()
                                .tipo(TipoFinanciero.EGRESO)
                                .metodoPago(metodoPago)
                                .monto(montoPagado)
                                .descripcion(truncar("Pago proveedor " + razonSocialProveedor + " (" + numeroComprobante + ")", 200))
                                .referenciaExterna(numeroComprobante)
                                .compraId(compraId)
                                .categoriaFinancieraId(catCompra.getId())
                                .build()
                ));
    }

    private void amortizarCuentaPorPagarCompra(Long compraId, String numeroComprobante, BigDecimal montoPagado, MetodoPago metodoPago) {
        buscarCuentaPorPagar(compraId, numeroComprobante).ifPresent(cuenta -> {
            if (cuenta.getEstado() == EstadoCuentaPorPagar.PAGADO_TOTAL || cuenta.getEstado() == EstadoCuentaPorPagar.ANULADO) {
                return;
            }
            BigDecimal montoAplicable = montoPagado.min(cuenta.getSaldoPendiente());
            if (esMontoPositivo(montoAplicable)) {
                cuenta.aplicarPago(montoAplicable, metodoPago, null, numeroComprobante, "Pago registrado desde Compras", LocalDateTime.now());
                cuentaPorPagarRepository.save(cuenta);
            }
        });
    }

    private void actualizarTransaccionCompraPorPago(
            Long compraId,
            String numeroComprobante,
            String razonSocialProveedor,
            BigDecimal montoPagado,
            boolean liquidadaTotalmente,
            CategoriaFinanciera catCompra) {
        TransaccionFinanciera existente = buscarTransaccionCompra(compraId, numeroComprobante).orElse(null);

        if (existente != null && liquidadaTotalmente) {
            existente.setEstado(EstadoFinanciero.PAGADO);
            transaccionRepository.save(existente);
        } else if (existente == null) {
            guardarTransaccionContable(
                    generarCodigoUnico("TRX-PAG", numeroComprobante),
                    truncar("Amortización compra " + numeroComprobante + " - " + razonSocialProveedor, 195),
                    montoPagado,
                    LocalDate.now(),
                    TipoFinanciero.EGRESO,
                    EstadoFinanciero.PAGADO,
                    "FACTURA",
                    numeroComprobante,
                    null,
                    compraId,
                    catCompra
            );
        }
    }

    private Optional<CuentaPorPagar> buscarCuentaPorPagar(Long compraId, String numeroComprobante) {
        if (compraId != null) {
            Optional<CuentaPorPagar> porCompra = cuentaPorPagarRepository.findFirstByCompraId(compraId);
            if (porCompra.isPresent()) {
                return porCompra;
            }
        }
        if (numeroComprobante != null && !numeroComprobante.isBlank()) {
            return cuentaPorPagarRepository.findFirstByNumeroComprobante(numeroComprobante);
        }
        return Optional.empty();
    }

    private Optional<TransaccionFinanciera> buscarTransaccionCompra(Long compraId, String numeroComprobante) {
        if (compraId != null) {
            Optional<TransaccionFinanciera> porCompra = transaccionRepository.findFirstByCompraId(compraId);
            if (porCompra.isPresent()) {
                return porCompra;
            }
        }
        if (numeroComprobante != null && !numeroComprobante.isBlank()) {
            return transaccionRepository.findFirstByNumeroComprobante(numeroComprobante);
        }
        return Optional.empty();
    }

    private void guardarTransaccionContable(
            String codigo,
            String concepto,
            BigDecimal monto,
            LocalDate fecha,
            TipoFinanciero tipo,
            EstadoFinanciero estado,
            String tipoComprobante,
            String numeroComprobante,
            Long ventaId,
            Long compraId,
            CategoriaFinanciera categoria) {
        TransaccionFinanciera trx = new TransaccionFinanciera();
        trx.setCodigo(codigo);
        trx.setConcepto(concepto);
        trx.setMonto(monto);
        trx.setFechaTransaccion(fecha);
        trx.setTipo(tipo);
        trx.setEstado(estado);
        trx.setTipoComprobante(tipoComprobante);
        trx.setNumeroComprobante(numeroComprobante);
        trx.setVentaId(ventaId);
        trx.setCompraId(compraId);
        trx.setCategoriaFinanciera(categoria);
        transaccionRepository.save(trx);
    }

    private CategoriaFinanciera obtenerOCrearCategoria(TipoFinanciero tipo, String nombreFallback) {
        return categoriaRepository.findByNombre(nombreFallback)
                .orElseGet(() -> {
                    CategoriaFinanciera nueva = new CategoriaFinanciera();
                    nueva.setNombre(nombreFallback);
                    nueva.setDescripcion("Categoría autogenerada para operaciones integradas del ERP");
                    nueva.setTipoDefault(tipo);
                    return categoriaRepository.save(nueva);
                });
    }

    private boolean esMontoPositivo(BigDecimal monto) {
        return monto != null && monto.compareTo(BigDecimal.ZERO) > 0;
    }

    private String resolverNombreCliente(String clienteNombre) {
        return (clienteNombre != null && !clienteNombre.isBlank()) ? clienteNombre : "Cliente General";
    }

    private String generarCodigoUnico(String prefijo, String referencia) {
        String cleanRef = referencia != null ? referencia.replaceAll("[^A-Za-z0-9]", "") : "";
        String suffix = cleanRef.length() > 12 ? cleanRef.substring(0, 12) : cleanRef;
        String unique = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return truncar(prefijo + "-" + suffix + "-" + unique, 48);
    }

    private String truncar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
