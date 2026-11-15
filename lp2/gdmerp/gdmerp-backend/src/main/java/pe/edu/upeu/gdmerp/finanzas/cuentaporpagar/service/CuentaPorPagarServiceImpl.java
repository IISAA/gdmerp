package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.BusinessConflictException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CrearCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.RegistrarPagoCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.CuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.mapper.CuentaPorPagarMapper;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.repository.CuentaPorPagarRepository;
import pe.edu.upeu.gdmerp.finanzas.mapper.FinanzasMappingHelper;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.EstadoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MovimientoCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.SesionCaja;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.repository.SesionCajaRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.CategoriaFinancieraRepository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.repository.TransaccionFinancieraRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaPorPagarServiceImpl implements CuentaPorPagarService {

    private static final String CATEGORIA_COMPRAS = "Compras e Insumos";

    private final CuentaPorPagarRepository cuentaPorPagarRepository;
    private final SesionCajaRepository sesionCajaRepository;
    private final CategoriaFinancieraRepository categoriaRepository;
    private final TransaccionFinancieraRepository transaccionRepository;
    private final CuentaPorPagarMapper cuentaPorPagarMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CuentaPorPagarResponse> listarTodos(EstadoCuentaPorPagar estado) {
        List<CuentaPorPagar> cuentas = (estado != null)
                ? cuentaPorPagarRepository.findByEstadoOrderByFechaVencimientoAsc(estado)
                : cuentaPorPagarRepository.findAllByOrderByFechaVencimientoAsc();
        return cuentaPorPagarMapper.toResponseList(
                cuentas, FinanzasMappingHelper.toCountMap(cuentaPorPagarRepository.contarAbonosPorCuenta()));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaPorPagarDetalleResponse obtenerPorId(Long id) {
        CuentaPorPagar cuenta = cuentaPorPagarRepository.findWithAbonosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada con ID: " + id));
        return cuentaPorPagarMapper.toDetalleResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaPorPagarResponse registrarCuentaPorPagar(CrearCuentaPorPagarRequest req) {
        String codigo = resolverCodigoUnico(req.getCodigo());
        LocalDate emision = req.getFechaEmision() != null ? req.getFechaEmision() : LocalDate.now();
        validarFechasEmisionVencimiento(emision, req.getFechaVencimiento());

        CuentaPorPagar cuenta = CuentaPorPagar.builder()
                .codigo(codigo)
                .proveedorId(req.getProveedorId())
                .compraId(req.getCompraId())
                .proveedorRazonSocial(req.getProveedorRazonSocial().trim())
                .proveedorDocumento(req.getProveedorDocumento())
                .numeroComprobante(req.getNumeroComprobante())
                .descripcion(req.getDescripcion())
                .montoTotal(req.getMontoTotal())
                .saldoPendiente(req.getMontoTotal())
                .fechaEmision(emision)
                .fechaVencimiento(req.getFechaVencimiento())
                .estado(EstadoCuentaPorPagar.PENDIENTE)
                .recepcionConfirmada(false)
                .build();

        return cuentaPorPagarMapper.toSummaryResponse(cuentaPorPagarRepository.save(cuenta));
    }

    @Override
    @Transactional
    public CuentaPorPagarDetalleResponse registrarPago(Long id, RegistrarPagoCuentaPorPagarRequest req) {
        CuentaPorPagar cuenta = cuentaPorPagarRepository.findWithAbonosById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta por pagar no encontrada con ID: " + id));

        cuenta.aplicarPago(
                req.getMonto(),
                req.getMetodoPago(),
                req.getSesionCajaId(),
                req.getNumeroOperacion(),
                req.getNota(),
                LocalDateTime.now()
        );

        CuentaPorPagar actualizada = cuentaPorPagarRepository.save(cuenta);
        impactarMovimientoEgresoEnCaja(actualizada, req);
        actualizarLibroContablePorPago(actualizada, req);

        log.info("[Finanzas] Pago de {} aplicado a CuentaPorPagar {}. Nuevo saldo: {} ({})",
                req.getMonto(), actualizada.getCodigo(), actualizada.getSaldoPendiente(), actualizada.getEstado());

        return cuentaPorPagarMapper.toDetalleResponse(actualizada);
    }

    private String resolverCodigoUnico(String codigoSolicitado) {
        if (codigoSolicitado != null && !codigoSolicitado.isBlank()) {
            String limpio = codigoSolicitado.trim();
            if (cuentaPorPagarRepository.findByCodigo(limpio).isPresent()) {
                throw new BusinessConflictException("Ya existe una cuenta por pagar con el código: " + limpio);
            }
            return limpio;
        }
        return "CXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validarFechasEmisionVencimiento(LocalDate emision, LocalDate vencimiento) {
        if (vencimiento != null && vencimiento.isBefore(emision)) {
            throw new BusinessConflictException("La fecha de vencimiento no puede ser anterior a la fecha de emisión.");
        }
    }

    private void impactarMovimientoEgresoEnCaja(CuentaPorPagar cuenta, RegistrarPagoCuentaPorPagarRequest req) {
        if (req.getSesionCajaId() == null) {
            return;
        }
        SesionCaja sesion = sesionCajaRepository.findById(req.getSesionCajaId())
                .orElseThrow(() -> new ResourceNotFoundException("Sesión de caja no encontrada con ID: " + req.getSesionCajaId()));

        if (sesion.getEstado() != EstadoCaja.ABIERTA) {
            throw new BusinessConflictException("No se puede registrar egreso en una sesión de caja cerrada: " + sesion.getCodigoSesion());
        }

        CategoriaFinanciera categoria = obtenerCategoriaCompras();
        MovimientoCaja movimiento = MovimientoCaja.builder()
                .fechaHora(LocalDateTime.now())
                .tipo(TipoFinanciero.EGRESO)
                .metodoPago(req.getMetodoPago() != null ? req.getMetodoPago() : MetodoPago.EFECTIVO)
                .monto(req.getMonto())
                .descripcion("Pago Cuenta por Pagar " + cuenta.getCodigo() + " - " + cuenta.getProveedorRazonSocial())
                .referenciaExterna(cuenta.getCodigo())
                .compraId(cuenta.getCompraId())
                .categoriaFinanciera(categoria)
                .build();

        sesion.agregarMovimiento(movimiento);
        sesionCajaRepository.save(sesion);
    }

    private void actualizarLibroContablePorPago(CuentaPorPagar cuenta, RegistrarPagoCuentaPorPagarRequest req) {
        if (cuenta.getEstado() == EstadoCuentaPorPagar.PAGADO_TOTAL && cuenta.getNumeroComprobante() != null) {
            transaccionRepository.findFirstByNumeroComprobante(cuenta.getNumeroComprobante())
                    .filter(trx -> trx.getTipo() == TipoFinanciero.EGRESO && trx.getEstado() == EstadoFinanciero.PENDIENTE)
                    .ifPresent(trx -> {
                        trx.setEstado(EstadoFinanciero.PAGADO);
                        transaccionRepository.save(trx);
                    });
            return;
        }

        TransaccionFinanciera pagoParcial = new TransaccionFinanciera();
        pagoParcial.setCodigo("PCXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        pagoParcial.setConcepto("Abono a proveedor " + cuenta.getProveedorRazonSocial() + " (" + cuenta.getCodigo() + ")");
        pagoParcial.setMonto(req.getMonto());
        pagoParcial.setFechaTransaccion(LocalDate.now());
        pagoParcial.setTipo(TipoFinanciero.EGRESO);
        pagoParcial.setEstado(EstadoFinanciero.PAGADO);
        pagoParcial.setTipoComprobante("PAGO_PROVEEDOR");
        pagoParcial.setNumeroComprobante(cuenta.getNumeroComprobante() != null ? cuenta.getNumeroComprobante() : cuenta.getCodigo());
        pagoParcial.setCompraId(cuenta.getCompraId());
        pagoParcial.setCategoriaFinanciera(obtenerCategoriaCompras());
        transaccionRepository.save(pagoParcial);
    }

    private CategoriaFinanciera obtenerCategoriaCompras() {
        return categoriaRepository.findByNombreIgnoreCase(CATEGORIA_COMPRAS)
                .orElseGet(() -> {
                    CategoriaFinanciera nueva = new CategoriaFinanciera();
                    nueva.setNombre(CATEGORIA_COMPRAS);
                    nueva.setDescripcion("Categoría automática para compras y cuentas por pagar a proveedores");
                    nueva.setTipoDefault(TipoFinanciero.EGRESO);
                    return categoriaRepository.save(nueva);
                });
    }
}
