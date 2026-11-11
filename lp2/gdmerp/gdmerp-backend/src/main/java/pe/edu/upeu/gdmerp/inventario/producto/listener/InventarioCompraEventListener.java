package pe.edu.upeu.gdmerp.inventario.producto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.inventario.producto.service.InventarioOperacionService;
import pe.edu.upeu.gdmerp.shared.event.RecepcionCompraEvent;

/**
 * Procesa el ingreso físico de mercadería emitido por el módulo Compras.
 * Se ejecuta en fase {@link TransactionPhase#BEFORE_COMMIT} para compartir la
 * transacción ACID: si la actualización de lote/stock falla, toda la recepción
 * en Compras realiza rollback automáticamente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventarioCompraEventListener {

    private final InventarioOperacionService inventarioOperacionService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onRecepcionCompra(RecepcionCompraEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            return;
        }
        log.info("Inventario: procesando recepción de compra {} ({} ítems) en fase BEFORE_COMMIT",
                event.numeroComprobante(), event.items().size());
        for (RecepcionCompraEvent.ItemRecepcion item : event.items()) {
            inventarioOperacionService.registrarIngresoLote(
                    item.productoId(),
                    item.cantidadRecibida(),
                    item.fechaVencimiento()
            );
        }
    }
}
