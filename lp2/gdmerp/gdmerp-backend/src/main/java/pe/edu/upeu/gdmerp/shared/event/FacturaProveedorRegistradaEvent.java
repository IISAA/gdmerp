package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;

/**
 * Workflow C: Facturación (Finanzas -> Compras).
 * Evento inmutable emitido por FinanzasService cuando el contador registra la factura física / XML del proveedor
 * y genera la Cuenta por Pagar correspondiente.
 * Notifica a Compras para actualizar la Orden de Compra al estado FACTURADA.
 */
public record FacturaProveedorRegistradaEvent(
        Long ordenId,
        BigDecimal montoTotal,
        String comprobanteFisico
) {
    public FacturaProveedorRegistradaEvent {
        if (ordenId == null) {
            throw new IllegalArgumentException("ordenId no puede ser nulo");
        }
        if (montoTotal == null || montoTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("montoTotal debe ser mayor a cero");
        }
        if (comprobanteFisico == null || comprobanteFisico.isBlank()) {
            throw new IllegalArgumentException("comprobanteFisico es obligatorio");
        }
    }
}
