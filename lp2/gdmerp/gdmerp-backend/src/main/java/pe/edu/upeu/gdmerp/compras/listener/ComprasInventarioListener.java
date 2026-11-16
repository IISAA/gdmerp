package pe.edu.upeu.gdmerp.compras.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.OrdenCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.repository.OrdenCompraRepository;
import pe.edu.upeu.gdmerp.exception.BusinessException;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.event.MercanciaRecibidaEvent;

/**
 * Workflow B: Listener en Compras.
 * Escucha MercanciaRecibidaEvent emitido por Inventario y actualiza el estado de la Orden a RECIBIDA.
 * Ejecuta en fase BEFORE_COMMIT para mantener la transacción atómica entre la recepción física y la orden.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComprasInventarioListener {

    private final OrdenCompraRepository ordenCompraRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onMercanciaRecibida(MercanciaRecibidaEvent event) {
        log.info("[Compras] Evento recibido: MercanciaRecibidaEvent para Orden ID: {} con fecha {}. Transicionando a RECIBIDA...",
                event.ordenId(), event.fechaRecepcion());

        OrdenCompra orden = ordenCompraRepository.findById(event.ordenId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + event.ordenId()));

        // Validación estricta de invariantes de negocio
        if (orden.getEstado() == EstadoCompra.ANULADA || orden.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("Transición rechazada: No se puede recibir mercancía para la orden cancelada ID: " + orden.getId());
        }
        if (orden.getEstado() == EstadoCompra.RECIBIDA) {
            log.warn("[Compras] La orden ID {} ya se encontraba en estado RECIBIDA. Operación idempotente.", orden.getId());
            return;
        }
        if (orden.getEstado() != EstadoCompra.CONFIRMADA) {
            throw new BusinessException("Transición inválida: La orden ID " + orden.getId()
                    + " debe estar en estado CONFIRMADA para marcarse como RECIBIDA. Estado actual: " + orden.getEstado());
        }

        orden.marcarRecibida(event.fechaRecepcion(), "ALMACENERO_EVENT_LISTENER");
        ordenCompraRepository.save(orden);

        log.info("[Compras] Orden de compra ID: {} transicionada exitosamente a RECIBIDA.", orden.getId());
    }
}
