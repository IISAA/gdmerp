package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.exception.StockConcurrencyException;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Operaciones atómicas sobre el stock.
 *
 * El control de concurrencia usa bloqueo optimista: las entidades Producto y Lote
 * llevan @Version, por lo que dos transacciones que lean el mismo estado y luego
 * actualicen producirán una OptimisticLockingFailureException en la que pierde.
 *
 * Para que el reintento realmente "re-intente", cada intento debe ejecutarse en una
 * transacción NUEVA. Por eso aquí NO se auto-proxifica el método @Transactional
 * mediante this.* (no pasaría por el proxy de AOP): los métodos orquestadores
 * (sin @Transactional) invocan las variantes transaccionales a través de un
 * ObjectProvider del propio servicio, de modo que cada llamada abre su propia
 * transacción y, si colisiona, se vuelve a abrir otra en el siguiente intento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioOperacionServiceImpl implements InventarioOperacionService {

    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;

    private final ObjectProvider<InventarioOperacionServiceImpl> selfProvider;

    @Value("${gdmerp.inventario.stock-retry.max-intentos:5}")
    private int maxIntentos;

    @Value("${gdmerp.inventario.stock-retry.backoff-ms:50}")
    private long backoffMs;

    @Override
    public void consumirStockFefo(Long productoId, Integer cantidadRequerida) {
        int intento = 0;
        while (true) {
            try {
                selfProvider.getObject().consumirStockFefoConTransaccion(productoId, cantidadRequerida);
                return;
            } catch (OptimisticLockingFailureException ex) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new StockConcurrencyException(
                            "La operación de consumo de stock no pudo completarse por concurrencia tras "
                                    + maxIntentos + " intentos. Intente nuevamente", ex);
                }
                log.warn("Consumo FEFO: colisión de bloqueo optimista en intento {} de {}", intento, maxIntentos);
                sleepBackoff();
            }
        }
    }

    @Override
    public String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        int intento = 0;
        while (true) {
            try {
                return selfProvider.getObject().registrarIngresoLoteConTransaccion(productoId, cantidad, fechaVencimiento);
            } catch (OptimisticLockingFailureException ex) {
                intento++;
                if (intento >= maxIntentos) {
                    throw new StockConcurrencyException(
                            "El ingreso de lote no pudo completarse por concurrencia tras "
                                    + maxIntentos + " intentos. Intente nuevamente", ex);
                }
                log.warn("Ingreso de lote: colisión de bloqueo optimista en intento {} de {}", intento, maxIntentos);
                sleepBackoff();
            }
        }
    }

    @Transactional
    public void consumirStockFefoConTransaccion(Long productoId, Integer cantidadRequerida) {
        if (cantidadRequerida == null || cantidadRequerida < 1) {
            throw new IllegalArgumentException("La cantidad requerida debe ser mayor a cero");
        }

        Producto producto = getProducto(productoId);

        if (producto.getStockTotal() < cantidadRequerida) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        List<Lote> lotesDisponibles = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(productoId, 0);

        int cantidadPendiente = cantidadRequerida;

        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente <= 0) break;

            int cantidadAExtraer = Math.min(lote.getCantidadActual(), cantidadPendiente);
            lote.setCantidadActual(lote.getCantidadActual() - cantidadAExtraer);

            cantidadPendiente -= cantidadAExtraer;
        }

        if (cantidadPendiente > 0) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        producto.setStockTotal(producto.getStockTotal() - cantidadRequerida);
    }

    @Transactional
    public String registrarIngresoLoteConTransaccion(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        if (cantidad == null || cantidad < 1) {
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
        nuevoLote.setCantidadActual(cantidad);
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);

        producto.setStockTotal(producto.getStockTotal() + cantidad);

        return numeroLote;
    }

    private Producto getProducto(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }

    private void sleepBackoff() {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new StockConcurrencyException("Reintento de operación de stock interrumpido", ie);
        }
    }
}