package pe.edu.upeu.gdmerp.inventario.producto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.shared.event.OrdenProduccionAprobadaEvent;
import pe.edu.upeu.gdmerp.shared.event.OrdenProduccionCompletadaEvent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Listener de integración en Inventario para órdenes de producción (Producción -> Inventario).
 * Ejecuta en fase {@link TransactionPhase#BEFORE_COMMIT} con fallbackExecution = true
 * para garantizar atomicidad ACID compartida en Oracle entre Producción e Inventario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioProduccionEventListener {

    private final InventarioOperacionService inventarioOperacionService;
    private final AlmacenRepository almacenRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onOrdenProduccionAprobada(OrdenProduccionAprobadaEvent event) {
        if (event == null) {
            return;
        }
        log.info("Inventario: descontando {} insumo(s) por inicio de Orden de Producción #{} (CentroTrabajo: {}) en fase BEFORE_COMMIT",
                event.insumosConsumidos().size(), event.ordenId(), event.centroTrabajoId());

        List<InsumoDetalle> detalles = event.insumosConsumidos().stream()
                .map(insumo -> (InsumoDetalle) new InsumoEventoDetalle(
                        insumo.productoId(),
                        insumo.cantidadConsumida()
                ))
                .toList();

        inventarioOperacionService.descontarInsumosProduccion(detalles);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onOrdenProduccionCompletada(OrdenProduccionCompletadaEvent event) {
        if (event == null) {
            return;
        }
        if (event.productoTerminadoId() == null) {
            throw new IllegalArgumentException(
                    "El producto terminado es obligatorio al completar la orden de producción #" + event.ordenId());
        }
        if (event.cantidadProducida() == null || event.cantidadProducida().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad producida debe ser mayor a cero para la orden de producción #" + event.ordenId());
        }
        if (event.almacenDestinoId() != null && !almacenRepository.existsById(event.almacenDestinoId())) {
            throw new ResourceNotFoundException(
                    "Almacén destino no encontrado con ID: " + event.almacenDestinoId());
        }

        String codigoLote = event.ordenId() != null
                ? "LT-OP-" + event.ordenId()
                : null;

        log.info("Inventario: ingresando {} unidades del producto terminado ID {} (Orden #{}, Almacén destino: {}) en fase BEFORE_COMMIT",
                event.cantidadProducida(), event.productoTerminadoId(), event.ordenId(), event.almacenDestinoId());

        inventarioOperacionService.registrarIngresoProduccion(
                event.productoTerminadoId(),
                event.cantidadProducida(),
                codigoLote
        );
    }

    private record InsumoEventoDetalle(Long getMateriaPrimaId, BigDecimal getCantidadRequerida)
            implements InsumoDetalle {
    }
}
