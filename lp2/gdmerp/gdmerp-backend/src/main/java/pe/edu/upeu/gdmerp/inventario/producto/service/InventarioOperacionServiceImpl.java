package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioOperacionServiceImpl implements InventarioOperacionService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ObjectProvider<InventarioOperacionServiceImpl> selfProvider;

    @Value("${gdmerp.inventario.stock-retry.max-intentos:5}")
    private int maxIntentos;

    @Value("${gdmerp.inventario.stock-retry.backoff-ms:50}")
    private long backoffMs;

    @Override
    public void consumirStockFefo(Long productoId, BigDecimal cantidadRequerida) {
        int intentos = 0;
        while (true) {
            try {
                selfProvider.getObject().consumirStockFefoConTransaccion(productoId, cantidadRequerida);
                return;
            } catch (OptimisticLockingFailureException ex) {
                intentos++;
                if (intentos >= maxIntentos) {
                    log.error(
                            "Excedido el número máximo de reintentos ({}) por bloqueo optimista al consumir stock del producto ID: {}",
                            maxIntentos, productoId);
                    throw new IllegalStateException(
                            "No fue posible completar la operación debido a concurrencia. Por favor intente nuevamente.",
                            ex);
                }
                log.warn("Colisión de concurrencia detectada en producto ID: {}. Reintento {}/{}...",
                        productoId, intentos, maxIntentos);
                dormir(intentos);
            }
        }
    }

    @Override
    public void consumirStockFefo(Long productoId, Integer cantidadRequerida) {
        consumirStockFefo(productoId, cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }

    @Override
    public String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        return registrarIngresoLote(productoId, cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                fechaVencimiento);
    }

    @Override
    public String registrarIngresoLote(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        int intentos = 0;
        while (true) {
            try {
                return selfProvider.getObject().registrarIngresoLoteConTransaccion(productoId, cantidad,
                        fechaVencimiento);
            } catch (OptimisticLockingFailureException ex) {
                intentos++;
                if (intentos >= maxIntentos) {
                    log.error(
                            "Excedido el número máximo de reintentos ({}) por bloqueo optimista al ingresar stock del producto ID: {}",
                            maxIntentos, productoId);
                    throw new IllegalStateException(
                            "No fue posible registrar el lote debido a concurrencia. Por favor intente nuevamente.",
                            ex);
                }
                log.warn("Colisión de concurrencia en producto ID: {}. Reintento {}/{}...",
                        productoId, intentos, maxIntentos);
                dormir(intentos);
            }
        }
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, Integer cantidadRequerida) {
        consumirStockFefoConTransaccion(productoId,
                cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }

        Producto producto = getProducto(productoId);
        BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;

        if (stockAnterior.compareTo(cantidadRequerida) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para el insumo: " + producto.getNombre() +
                            ". Requerido: " + cantidadRequerida + ", Disponible: " + stockAnterior);
        }

        List<Lote> lotesDisponibles = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(productoId, BigDecimal.ZERO);

        BigDecimal cantidadPendiente = cantidadRequerida;

        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente.compareTo(BigDecimal.ZERO) <= 0)
                break;

            BigDecimal disponibleLote = lote.getCantidadActual() != null ? lote.getCantidadActual() : BigDecimal.ZERO;
            BigDecimal cantidadAExtraer = disponibleLote.min(cantidadPendiente);

            lote.setCantidadActual(disponibleLote.subtract(cantidadAExtraer).setScale(4, RoundingMode.HALF_UP));
            cantidadPendiente = cantidadPendiente.subtract(cantidadAExtraer);
        }

        if (cantidadPendiente.compareTo(BigDecimal.ZERO) > 0) {
            if (stockAnterior.compareTo(cantidadPendiente) >= 0) {
                Lote loteAuto = new Lote();
                loteAuto.setProducto(producto);
                loteAuto.setNumeroLote("LT-AUTO-" + producto.getId() + "-" + (System.currentTimeMillis() % 100000));
                loteAuto.setFechaProduccion(LocalDate.now());
                loteAuto.setFechaVencimiento(LocalDate.now().plusYears(1));
                loteAuto.setCantidadActual(BigDecimal.ZERO);
                loteRepository.save(loteAuto);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente en lotes para el insumo: " + producto.getNombre() +
                                ". Requerido: " + cantidadRequerida + ", Disponible: " + stockAnterior);
            }
        }

        BigDecimal stockResultante = stockAnterior.subtract(cantidadRequerida).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);

        registrarKardex(producto, "SALIDA_PRODUCCION", cantidadRequerida, stockAnterior, stockResultante,
                "Consumo FEFO para orden de producción");
    }

    @Transactional
    public String registrarIngresoLoteConTransaccion(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ingreso debe ser mayor a cero");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (fechaVencimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a hoy");
        }

        Producto producto = getProducto(productoId);
        String numeroLote = generarCodigoLote();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad.setScale(4, RoundingMode.HALF_UP));
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);

        BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
        BigDecimal stockResultante = stockAnterior.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);

        registrarKardex(producto, "ENTRADA_LOTE", cantidad, stockAnterior, stockResultante,
                "Ingreso de lote " + numeroLote);

        return numeroLote;
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, Integer cantidad, String lote) {
        registrarIngresoProduccion(productoId, cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, BigDecimal cantidad, String lote) {
        Producto producto = getProducto(productoId);
        registrarIngresoProduccionInterno(producto, cantidad, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, Integer cantidad, String lote) {
        registrarIngresoProduccion(nombreProducto, cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad, String lote) {
        Producto producto = productoRepository.findByNombre(nombreProducto)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Producto no encontrado con nombre: " + nombreProducto));
        registrarIngresoProduccionInterno(producto, cantidad, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad) {
        registrarIngresoProduccion(nombreProducto, cantidad, null);
    }

    @Override
    @Transactional
    public void descontarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La orden de producción no contiene insumos para descontar");
        }

        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null)
                continue;
            BigDecimal cantidadRequerida = d.getCantidadRequerida() != null ? d.getCantidadRequerida()
                    : BigDecimal.ZERO;
            if (cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            Producto insumo = getProducto(d.getMateriaPrimaId());
            BigDecimal stockActual = insumo.getStockTotal() != null ? insumo.getStockTotal() : BigDecimal.ZERO;

            if (stockActual.compareTo(cantidadRequerida) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el insumo: " + insumo.getNombre() +
                                ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
            }
        }

        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null || d.getCantidadRequerida() == null)
                continue;
            if (d.getCantidadRequerida().compareTo(BigDecimal.ZERO) <= 0)
                continue;
            consumirStockFefoConTransaccion(d.getMateriaPrimaId(), d.getCantidadRequerida());
        }
    }

    @Override
    @Transactional
    public void reversarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty())
            return;

        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null || d.getCantidadRequerida() == null)
                continue;
            BigDecimal cantidadDevuelta = d.getCantidadRequerida();
            if (cantidadDevuelta.compareTo(BigDecimal.ZERO) <= 0)
                continue;

            Producto producto = getProducto(d.getMateriaPrimaId());
            BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
            BigDecimal stockResultante = stockAnterior.add(cantidadDevuelta).setScale(4, RoundingMode.HALF_UP);
            producto.setStockTotal(stockResultante);

            // Sincronizar también el saldo de lotes para mantener coherencia FEFO
            List<Lote> lotesActivos = loteRepository
                    .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(producto.getId(),
                            BigDecimal.ZERO);

            if (!lotesActivos.isEmpty()) {
                Lote loteDestino = lotesActivos.get(0);
                loteDestino.setCantidadActual(
                        loteDestino.getCantidadActual().add(cantidadDevuelta).setScale(4, RoundingMode.HALF_UP));
            } else {
                Lote loteRetorno = new Lote();
                loteRetorno.setProducto(producto);
                loteRetorno.setNumeroLote("LT-REV-" + producto.getId() + "-" + (System.currentTimeMillis() % 100000));
                loteRetorno.setFechaProduccion(LocalDate.now());
                loteRetorno.setFechaVencimiento(LocalDate.now().plusYears(1));
                loteRetorno.setCantidadActual(cantidadDevuelta.setScale(4, RoundingMode.HALF_UP));
                loteRepository.save(loteRetorno);
            }

            registrarKardex(producto, "DEVOLUCION_PRODUCCION", cantidadDevuelta, stockAnterior, stockResultante,
                    "Devolución por edición/cancelación de orden planificada");

            log.info("Stock revertido para insumo '{}' (ID: {}). Cantidad devuelta: {}, nuevo stock: {}",
                    producto.getNombre(), producto.getId(), cantidadDevuelta, stockResultante);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

    private void registrarIngresoProduccionInterno(Producto producto, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad fabricada debe ser mayor a cero");
        }

        String numeroLote = (lote != null && !lote.isBlank()) ? lote : generarCodigoLote();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad.setScale(4, RoundingMode.HALF_UP));
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(LocalDate.now().plusYears(1));
        loteRepository.save(nuevoLote);

        BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
        BigDecimal stockResultante = stockAnterior.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);

        registrarKardex(producto, "ENTRADA_PRODUCCION", cantidad, stockAnterior, stockResultante,
                "Ingreso de producto terminado - Lote " + numeroLote);
    }

    private void registrarKardex(Producto producto, String tipoMovimiento, BigDecimal cantidad,
            BigDecimal stockAnterior, BigDecimal stockResultante, String referencia) {
        BigDecimal costo = producto.getCostoPromedio() != null ? producto.getCostoPromedio() : BigDecimal.ZERO;
        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                tipoMovimiento,
                cantidad.setScale(4, RoundingMode.HALF_UP),
                stockAnterior.setScale(4, RoundingMode.HALF_UP),
                stockResultante.setScale(4, RoundingMode.HALF_UP),
                costo,
                referencia);
        movimientoRepository.save(movimiento);
    }

    private String generarCodigoLote() {
        return "LT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Producto getProducto(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + productoId));
    }

    private void dormir(int intento) {
        try {
            long jitter = (long) (Math.random() * backoffMs);
            Thread.sleep(backoffMs * intento + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo interrumpido durante espera de reintento", ie);
        }
    }
}