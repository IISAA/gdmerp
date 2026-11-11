package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.gdmerp.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.event.AjusteInventarioRealizadoEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioOperacionServiceImpl implements InventarioOperacionService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final ObjectProvider<InventarioOperacionServiceImpl> selfProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${gdmerp.inventario.stock-retry.max-intentos:5}")
    private int maxIntentos;

    @Value("${gdmerp.inventario.stock-retry.backoff-ms:50}")
    private long backoffMs;

    @Override
    public void consumirStockFefo(Long productoId, BigDecimal cantidadRequerida) {
        ejecutarConReintentoStock(productoId, "consumir stock del", () -> {
            selfProvider.getObject().consumirStockFefoConTransaccion(productoId, cantidadRequerida);
            return null;
        });
    }

    @Override
    public void consumirStockFefo(Long productoId, Integer cantidadRequerida) {
        consumirStockFefo(productoId, cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }

    @Override
    public String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        return registrarIngresoLote(
                productoId,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                fechaVencimiento
        );
    }

    @Override
    public String registrarIngresoLote(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        return ejecutarConReintentoStock(productoId, "registrar el lote del", () ->
                selfProvider.getObject().registrarIngresoLoteConTransaccion(productoId, cantidad, fechaVencimiento));
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, Integer cantidadRequerida) {
        consumirStockFefoConTransaccion(
                productoId,
                cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null
        );
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }

        Producto producto = getProducto(productoId);
        BigDecimal stockActual = obtenerStockSeguro(producto);

        if (stockActual.compareTo(cantidadRequerida) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para el insumo: " + producto.getNombre()
                            + ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
        }

        consumirLotesFefo(producto, cantidadRequerida.intValue(), stockActual, cantidadRequerida,
                "Stock insuficiente en lotes para el insumo: ", true);

        BigDecimal stockResultante = stockActual.subtract(cantidadRequerida).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);
        registrarMovimientoKardex(
                producto,
                "SALIDA_PRODUCCION",
                cantidadRequerida,
                stockActual,
                stockResultante,
                "PRODUCCION-FEFO"
        );
    }

    @Transactional
    public String registrarIngresoLoteConTransaccion(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        validarDatosIngresoLote(cantidad, fechaVencimiento);
        Producto producto = getProducto(productoId);

        String numeroLote = generarCodigoLote("LT");
        Lote nuevoLote = crearLote(producto, numeroLote, cantidad.intValue(), fechaVencimiento);
        loteRepository.save(nuevoLote);

        BigDecimal stockActual = obtenerStockSeguro(producto);
        producto.setStockTotal(stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP));

        return numeroLote;
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, Integer cantidad, String lote) {
        registrarIngresoProduccion(
                productoId,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                lote
        );
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(Long productoId, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad ingresada debe ser mayor a cero");
        }
        Producto producto = getProducto(productoId);
        registrarIngresoLoteProduccion(producto, cantidad, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, Integer cantidad, String lote) {
        registrarIngresoProduccion(
                nombreProducto,
                cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO,
                lote
        );
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad ingresada debe ser mayor a cero");
        }
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        Producto producto = productoRepository.findByNombre(nombreProducto.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con nombre: " + nombreProducto));
        registrarIngresoLoteProduccion(producto, cantidad, lote);
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
            if (!esDetalleValido(d)) {
                continue;
            }
            Producto insumo = getProducto(d.getMateriaPrimaId());
            BigDecimal stockActual = obtenerStockSeguro(insumo);
            if (stockActual.compareTo(d.getCantidadRequerida()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el insumo: " + insumo.getNombre()
                                + ". Requerido: " + d.getCantidadRequerida() + ", Disponible: " + stockActual);
            }
        }

        for (InsumoDetalle d : detalles) {
            if (esDetalleValido(d)) {
                consumirStockFefoConTransaccion(d.getMateriaPrimaId(), d.getCantidadRequerida());
            }
        }
    }

    @Override
    @Transactional
    public void reversarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        for (InsumoDetalle d : detalles) {
            if (!esDetalleValido(d)) {
                continue;
            }
            Producto producto = getProducto(d.getMateriaPrimaId());
            BigDecimal stockActual = obtenerStockSeguro(producto);
            producto.setStockTotal(stockActual.add(d.getCantidadRequerida()).setScale(4, RoundingMode.HALF_UP));
            log.info("Stock revertido para insumo '{}' (ID: {}). Cantidad devuelta: {}, nuevo stock: {}",
                    producto.getNombre(), producto.getId(), d.getCantidadRequerida(), producto.getStockTotal());
        }
    }

    @Override
    public void descontarStockVenta(Long productoId, Long loteId, Integer cantidad, String numeroTicket) {
        ejecutarConReintentoStock(productoId, "descontar stock de venta del", () -> {
            selfProvider.getObject().descontarStockVentaConTransaccion(productoId, loteId, cantidad, numeroTicket);
            return null;
        });
    }

    @Transactional
    public void descontarStockVentaConTransaccion(Long productoId, Long loteId, Integer cantidad, String numeroTicket) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad vendida debe ser mayor a cero");
        }

        Producto producto = getProducto(productoId);
        BigDecimal stockAnterior = obtenerStockSeguro(producto);
        BigDecimal requerido = BigDecimal.valueOf(cantidad);

        if (stockAnterior.compareTo(requerido) < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para el producto '" + producto.getNombre()
                            + "'. Requerido: " + requerido + ", Disponible: " + stockAnterior);
        }

        if (loteId != null) {
            descontarDeLoteEspecifico(producto, loteId, cantidad);
        } else {
            consumirLotesFefo(producto, cantidad, stockAnterior, requerido,
                    "Stock insuficiente en lotes para el producto: ", false);
        }

        BigDecimal stockResultante = stockAnterior.subtract(requerido).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);

        registrarMovimientoKardex(
                producto,
                "SALIDA_VENTA",
                requerido,
                stockAnterior,
                stockResultante,
                "VENTA:" + (numeroTicket != null ? numeroTicket : "SIN-TICKET")
        );
    }

    @Override
    public void reponerStockVenta(Long productoId, Integer cantidad, String numeroTicket) {
        ejecutarConReintentoStock(productoId, "reponer stock por anulación del", () -> {
            selfProvider.getObject().reponerStockVentaConTransaccion(productoId, cantidad, numeroTicket);
            return null;
        });
    }

    @Transactional
    public void reponerStockVentaConTransaccion(Long productoId, Integer cantidad, String numeroTicket) {
        if (cantidad == null || cantidad <= 0) {
            return;
        }

        Producto producto = getProducto(productoId);
        BigDecimal stockAnterior = obtenerStockSeguro(producto);
        BigDecimal reposicion = BigDecimal.valueOf(cantidad);
        BigDecimal stockResultante = stockAnterior.add(reposicion).setScale(4, RoundingMode.HALF_UP);

        producto.setStockTotal(stockResultante);
        reponerEnLoteExistenteOCrear(producto, cantidad);
        productoRepository.save(producto);

        registrarMovimientoKardex(
                producto,
                "ENTRADA_ANULACION_VENTA",
                reposicion,
                stockAnterior,
                stockResultante,
                "ANULACION:" + (numeroTicket != null ? numeroTicket : "SIN-TICKET")
        );
    }

    @Override
    public Long registrarAjusteInventario(Long productoId, BigDecimal diferencia, String motivoAjuste) {
        return ejecutarConReintentoStock(productoId, "registrar ajuste manual de inventario del", () ->
                selfProvider.getObject().registrarAjusteInventarioConTransaccion(productoId, diferencia, motivoAjuste)
        );
    }

    @Transactional
    public Long registrarAjusteInventarioConTransaccion(Long productoId, BigDecimal diferencia, String motivoAjuste) {
        if (productoId == null) {
            throw new IllegalArgumentException("El ID del producto es obligatorio para registrar un ajuste de inventario");
        }
        if (diferencia == null || diferencia.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("La diferencia de inventario no puede ser nula ni cero");
        }
        if (motivoAjuste == null || motivoAjuste.isBlank()) {
            throw new IllegalArgumentException("El motivo del ajuste o merma de inventario es obligatorio");
        }

        Producto producto = getProducto(productoId);
        BigDecimal stockAnterior = obtenerStockSeguro(producto);
        BigDecimal stockResultante = stockAnterior.add(diferencia).setScale(4, RoundingMode.HALF_UP);

        if (stockResultante.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientStockException(
                    "Stock insuficiente para registrar la merma/ajuste del producto '" + producto.getNombre()
                            + "'. Solicitado: " + diferencia.abs() + ", Disponible: " + stockAnterior);
        }

        boolean esSalida = diferencia.compareTo(BigDecimal.ZERO) < 0;
        if (esSalida) {
            int unidadesSalida = diferencia.abs().intValue();
            if (unidadesSalida > 0) {
                consumirLotesFefo(producto, unidadesSalida, stockAnterior, diferencia.abs(),
                        "Stock insuficiente en lotes para merma/ajuste del producto: ", false);
            }
        } else {
            int unidadesEntrada = diferencia.intValue();
            if (unidadesEntrada > 0) {
                reponerEnLoteExistenteOCrear(producto, unidadesEntrada);
            }
        }

        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);

        String tipoMovimiento = esSalida ? "MERMA_SALIDA" : "AJUSTE_ENTRADA";
        String motivoLimpio = motivoAjuste.trim();
        String referenciaCompleta = "AJUSTE: " + motivoLimpio;
        String referencia = referenciaCompleta.length() <= 120
                ? referenciaCompleta
                : referenciaCompleta.substring(0, 120);

        MovimientoInventario movimiento = registrarMovimientoKardex(
                producto,
                tipoMovimiento,
                diferencia.abs().setScale(4, RoundingMode.HALF_UP),
                stockAnterior,
                stockResultante,
                referencia
        );

        eventPublisher.publishEvent(new AjusteInventarioRealizadoEvent(
                movimiento.getId(),
                producto.getId(),
                diferencia,
                motivoLimpio
        ));

        return movimiento.getId();
    }

    private void registrarIngresoLoteProduccion(Producto producto, BigDecimal cantidad, String lote) {
        BigDecimal stockActual = obtenerStockSeguro(producto);
        BigDecimal stockResultante = stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);

        String numeroLote = (lote != null && !lote.isBlank()) ? lote : generarCodigoLote("LT");
        Lote nuevoLote = crearLote(producto, numeroLote, cantidad.intValue(), LocalDate.now().plusYears(1));
        loteRepository.save(nuevoLote);

        registrarMovimientoKardex(
                producto,
                "ENTRADA_PRODUCCION",
                cantidad,
                stockActual,
                stockResultante,
                "PRODUCCION-LOTE:" + numeroLote
        );
    }

    private void descontarDeLoteEspecifico(Producto producto, Long loteId, int cantidad) {
        Lote lote = loteRepository.findById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado con ID: " + loteId));
        if (!lote.getProducto().getId().equals(producto.getId())) {
            throw new IllegalArgumentException(
                    "El lote " + loteId + " no corresponde al producto '" + producto.getNombre() + "'");
        }
        int disponibleLote = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
        if (disponibleLote < cantidad) {
            throw new InsufficientStockException(
                    "Stock insuficiente en el lote " + lote.getNumeroLote()
                            + " del producto '" + producto.getNombre() + "'. Disponible: " + disponibleLote);
        }
        lote.setCantidadActual(disponibleLote - cantidad);
        loteRepository.save(lote);
    }

    private void consumirLotesFefo(
            Producto producto,
            int cantidadObjetivo,
            BigDecimal stockDisponible,
            BigDecimal requerido,
            String prefijoError,
            boolean lanzarResponseStatus) {
        List<Lote> lotesDisponibles = loteRepository
                .findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(producto.getId(), 0);

        int cantidadPendiente = cantidadObjetivo;
        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente <= 0) {
                break;
            }
            int cantidadAExtraer = Math.min(lote.getCantidadActual(), cantidadPendiente);
            lote.setCantidadActual(lote.getCantidadActual() - cantidadAExtraer);
            cantidadPendiente -= cantidadAExtraer;
        }

        if (cantidadPendiente > 0) {
            if (stockDisponible.compareTo(BigDecimal.valueOf(cantidadPendiente)) >= 0) {
                Lote loteAuto = new Lote();
                loteAuto.setProducto(producto);
                loteAuto.setNumeroLote("LT-AUTO-" + producto.getId() + "-" + (System.currentTimeMillis() % 100000));
                loteAuto.setFechaProduccion(LocalDate.now());
                loteAuto.setFechaVencimiento(LocalDate.now().plusYears(1));
                loteAuto.setCantidadActual(0);
                loteRepository.save(loteAuto);
                return;
            }
            String mensaje = prefijoError + producto.getNombre()
                    + ". Requerido: " + requerido + ", Disponible: " + stockDisponible;
            if (lanzarResponseStatus) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
            }
            throw new InsufficientStockException(mensaje);
        }
    }

    private void reponerEnLoteExistenteOCrear(Producto producto, int cantidad) {
        List<Lote> lotes = loteRepository.findByProductoId(producto.getId());
        if (!lotes.isEmpty()) {
            Lote ultimo = lotes.get(lotes.size() - 1);
            int actual = ultimo.getCantidadActual() != null ? ultimo.getCantidadActual() : 0;
            ultimo.setCantidadActual(actual + cantidad);
            loteRepository.save(ultimo);
            return;
        }
        Lote loteRecuperacion = crearLote(
                producto,
                "LT-ANUL-" + producto.getId() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                cantidad,
                LocalDate.now().plusYears(1)
        );
        loteRepository.save(loteRecuperacion);
    }

    private MovimientoInventario registrarMovimientoKardex(
            Producto producto,
            String tipoMovimiento,
            BigDecimal cantidad,
            BigDecimal stockAnterior,
            BigDecimal stockResultante,
            String referencia) {
        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                tipoMovimiento,
                cantidad,
                stockAnterior,
                stockResultante,
                producto.getCostoPromedio(),
                referencia
        );
        return movimientoInventarioRepository.save(movimiento);
    }

    private <T> T ejecutarConReintentoStock(Long productoId, String accionDescripcion, Supplier<T> accion) {
        int intentos = 0;
        while (true) {
            try {
                return accion.get();
            } catch (OptimisticLockingFailureException ex) {
                intentos++;
                if (intentos >= maxIntentos) {
                    log.error("Excedido el número máximo de reintentos ({}) por bloqueo optimista al {} producto ID: {}",
                            maxIntentos, accionDescripcion, productoId);
                    throw new IllegalStateException(
                            "No fue posible completar la operación debido a concurrencia. Por favor intente nuevamente.",
                            ex
                    );
                }
                log.warn("Colisión de concurrencia detectada en producto ID: {}. Reintento {}/{}...",
                        productoId, intentos, maxIntentos);
                dormir(intentos);
            }
        }
    }

    private void validarDatosIngresoLote(BigDecimal cantidad, LocalDate fechaVencimiento) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad de ingreso debe ser mayor a cero");
        }
        if (fechaVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (fechaVencimiento.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a hoy");
        }
    }

    private boolean esDetalleValido(InsumoDetalle d) {
        return d != null
                && d.getMateriaPrimaId() != null
                && d.getCantidadRequerida() != null
                && d.getCantidadRequerida().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal obtenerStockSeguro(Producto producto) {
        return producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
    }

    private String generarCodigoLote(String prefijo) {
        return prefijo + "-" + LocalDate.now().getYear() + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private Lote crearLote(Producto producto, String numeroLote, int cantidadActual, LocalDate fechaVencimiento) {
        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidadActual);
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        return nuevoLote;
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
