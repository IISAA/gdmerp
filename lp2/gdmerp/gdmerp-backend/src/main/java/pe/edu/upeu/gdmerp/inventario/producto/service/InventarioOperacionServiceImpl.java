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
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioOperacionServiceImpl implements InventarioOperacionService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;
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
                    log.error("Excedido el número máximo de reintentos ({}) por bloqueo optimista al consumir stock del producto ID: {}",
                            maxIntentos, productoId);
                    throw new IllegalStateException("No fue posible completar la operación debido a concurrencia. Por favor intente nuevamente.", ex);
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
        return registrarIngresoLote(productoId, cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO, fechaVencimiento);
    }

    @Override
    public String registrarIngresoLote(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento) {
        int intentos = 0;
        while (true) {
            try {
                return selfProvider.getObject().registrarIngresoLoteConTransaccion(productoId, cantidad, fechaVencimiento);
            } catch (OptimisticLockingFailureException ex) {
                intentos++;
                if (intentos >= maxIntentos) {
                    log.error("Excedido el número máximo de reintentos ({}) por bloqueo optimista al ingresar stock del producto ID: {}",
                            maxIntentos, productoId);
                    throw new IllegalStateException("No fue posible registrar el lote debido a concurrencia. Por favor intente nuevamente.", ex);
                }
                log.warn("Colisión de concurrencia en producto ID: {}. Reintento {}/{}...",
                        productoId, intentos, maxIntentos);
                dormir(intentos);
            }
        }
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, Integer cantidadRequerida) {
        consumirStockFefoConTransaccion(productoId, cantidadRequerida != null ? BigDecimal.valueOf(cantidadRequerida) : null);
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, BigDecimal cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }

        Producto producto = getProducto(productoId);
        BigDecimal requerido = cantidadRequerida;

        BigDecimal stockActual = (producto.getStockActual() != null) ? producto.getStockActual() : BigDecimal.ZERO;

        if (stockActual.compareTo(requerido) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuficiente para el insumo: " + producto.getNombre() +
                    ". Requerido: " + requerido + ", Disponible: " + stockActual);
        }

        List<Lote> lotesDisponibles = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(productoId, 0);

        int cantidadPendiente = cantidadRequerida.intValue();

        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente <= 0) break;

            int cantidadAExtraer = Math.min(lote.getCantidadActual(), cantidadPendiente);
            lote.setCantidadActual(lote.getCantidadActual() - cantidadAExtraer);

            cantidadPendiente -= cantidadAExtraer;
        }

        if (cantidadPendiente > 0) {
            BigDecimal stockRemanente = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
            if (stockRemanente.compareTo(BigDecimal.valueOf(cantidadPendiente)) >= 0) {
                Lote loteAuto = new Lote();
                loteAuto.setProducto(producto);
                loteAuto.setNumeroLote("LT-AUTO-" + producto.getId() + "-" + (System.currentTimeMillis() % 100000));
                loteAuto.setFechaProduccion(LocalDate.now());
                loteAuto.setFechaVencimiento(LocalDate.now().plusYears(1));
                loteAuto.setCantidadActual(0);
                loteRepository.save(loteAuto);
                cantidadPendiente = 0;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente en lotes para el insumo: " + producto.getNombre() +
                        ". Requerido: " + requerido + ", Disponible: " + stockActual);
            }
        }

        BigDecimal actual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(actual.subtract(requerido).setScale(4, RoundingMode.HALF_UP));
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

        String numeroLote = "LT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad.intValue());
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP));

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
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad fabricada debe ser mayor a cero");
        }
        Producto producto = getProducto(productoId);
        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP));

        LocalDate fechaVencimiento = LocalDate.now().plusYears(1);
        String numeroLote = (lote != null && !lote.isBlank())
                ? lote
                : "LT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad.intValue());
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, Integer cantidad, String lote) {
        registrarIngresoProduccion(nombreProducto, cantidad != null ? BigDecimal.valueOf(cantidad) : BigDecimal.ZERO, lote);
    }

    @Override
    @Transactional
    public void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad, String lote) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad fabricada debe ser mayor a cero");
        }
        Producto producto = productoRepository.findByNombre(nombreProducto)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con nombre: " + nombreProducto));

        BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(stockActual.add(cantidad).setScale(4, RoundingMode.HALF_UP));

        LocalDate fechaVencimiento = LocalDate.now().plusYears(1);
        String numeroLote = (lote != null && !lote.isBlank())
                ? lote
                : "LT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad.intValue());
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La orden de producción no contiene insumos para descontar");
        }

        // 1. Validación previa completa con defensa contra nulos y verificación de saldo disponible
        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null) {
                continue;
            }
            BigDecimal cantidadRequerida = d.getCantidadRequerida() != null ? d.getCantidadRequerida() : BigDecimal.ZERO;
            if (cantidadRequerida.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Producto insumo = getProducto(d.getMateriaPrimaId());
            BigDecimal stockActual = insumo.getStockActual() != null ? insumo.getStockActual() : BigDecimal.ZERO;

            if (stockActual.compareTo(cantidadRequerida) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Stock insuficiente para el insumo: " + insumo.getNombre() +
                        ". Requerido: " + cantidadRequerida + ", Disponible: " + stockActual);
            }
        }

        // 2. Ejecución atómica de la deducción por lotes FEFO
        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null || d.getCantidadRequerida() == null) {
                continue;
            }
            if (d.getCantidadRequerida().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            consumirStockFefoConTransaccion(d.getMateriaPrimaId(), d.getCantidadRequerida());
        }
    }

    @Override
    @Transactional
    public void reversarInsumosProduccion(List<? extends InsumoDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return;
        }
        for (InsumoDetalle d : detalles) {
            if (d == null || d.getMateriaPrimaId() == null || d.getCantidadRequerida() == null) {
                continue;
            }
            if (d.getCantidadRequerida().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Producto producto = getProducto(d.getMateriaPrimaId());
            BigDecimal stockActual = (producto.getStockTotal() != null) ? producto.getStockTotal() : BigDecimal.ZERO;
            producto.setStockTotal(stockActual.add(d.getCantidadRequerida()).setScale(4, RoundingMode.HALF_UP));
            log.info("Stock revertido para insumo '{}' (ID: {}). Cantidad devuelta: {}, nuevo stock: {}",
                    producto.getNombre(), producto.getId(), d.getCantidadRequerida(), producto.getStockTotal());
        }
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
