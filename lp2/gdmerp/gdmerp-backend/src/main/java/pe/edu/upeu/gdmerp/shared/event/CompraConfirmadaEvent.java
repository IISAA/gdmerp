package pe.edu.upeu.gdmerp.shared.event;

import java.util.List;

/**
 * Workflow A: Confirmación (Compras -> Inventario).
 * Evento inmutable emitido por ComprasService cuando la Orden de Compra cambia al estado CONFIRMADA.
 * Notifica al módulo de Inventario para generar la recepción física (Albarán de Entrada) en estado PENDIENTE.
 */
public record CompraConfirmadaEvent(
        Long ordenId,
        Long proveedorId,
        List<InsumoEventDto> insumos
) {
    public CompraConfirmadaEvent {
        if (ordenId == null) {
            throw new IllegalArgumentException("ordenId no puede ser nulo");
        }
        if (proveedorId == null) {
            throw new IllegalArgumentException("proveedorId no puede ser nulo");
        }
        if (insumos == null || insumos.isEmpty()) {
            throw new IllegalArgumentException("La orden de compra confirmada debe contener al menos un insumo");
        }
        insumos = List.copyOf(insumos);
    }
}
