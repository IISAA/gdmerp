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
import pe.edu.upeu.gdmerp.shared.event.FacturaProveedorRegistradaEvent;

/**
 * Workflow C: Listener en Compras.
 * Escucha FacturaProveedorRegistradaEvent emitido por Finanzas y actualiza el estado de la Orden a FACTURADA.
 *
 * Ejecuta en fase BEFORE_COMMIT: Si Compras rechaza la facturación (por ejemplo, porque la mercancía
 * no ha sido recibida físicamente todavía según la regla de 3-way matching), la creación de la
 * Factura y Cuenta por Pagar en Finanzas sufrirá un ROLLBACK automático.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComprasFinanzasListener {

    private final OrdenCompraRepository ordenCompraRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onFacturaProveedorRegistrada(FacturaProveedorRegistradaEvent event) {
        log.info("[Compras] Evento recibido: FacturaProveedorRegistradaEvent para Orden ID: {} con comprobante {}. Monto: {}. Transicionando a FACTURADA...",
                event.ordenId(), event.comprobanteFisico(), event.montoTotal());

        OrdenCompra orden = ordenCompraRepository.findById(event.ordenId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden de compra no encontrada con ID: " + event.ordenId()));

        // Validación estricta de invariantes de negocio (3-Way Matching: Orden -> Recepción Física -> Factura)
        if (orden.getEstado() == EstadoCompra.ANULADA || orden.getEstado() == EstadoCompra.CANCELADA) {
            throw new BusinessException("Transición rechazada: No se puede facturar una orden cancelada ID: " + orden.getId());
        }
        if (orden.getEstado() == EstadoCompra.FACTURADA) {
            log.warn("[Compras] La orden ID {} ya se encontraba en estado FACTURADA. Operación idempotente.", orden.getId());
            return;
        }
        if (orden.getEstado() != EstadoCompra.RECIBIDA) {
            throw new BusinessException("Transición inválida (3-Way Matching): La orden ID " + orden.getId()
                    + " debe estar en estado RECIBIDA físicamente antes de ser FACTURADA. Estado actual: " + orden.getEstado());
        }

        orden.marcarFacturada(event.comprobanteFisico(), "CONTADOR_EVENT_LISTENER");
        ordenCompraRepository.save(orden);

        log.info("[Compras] Orden de compra ID: {} transicionada exitosamente a FACTURADA.", orden.getId());
    }
}
