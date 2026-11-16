package pe.edu.upeu.gdmerp.shared.event;

import java.time.LocalDateTime;

/**
 * Workflow B: Recepción Física (Inventario -> Compras).
 * Evento inmutable emitido por InventarioService cuando el almacenero valida la recepción física
 * y se actualiza el Kardex / lotes / stock.
 * Notifica a Compras para actualizar la Orden de Compra al estado RECIBIDA.
 */
public record MercanciaRecibidaEvent(
        Long ordenId,
        LocalDateTime fechaRecepcion
) {
    public MercanciaRecibidaEvent {
        if (ordenId == null) {
            throw new IllegalArgumentException("ordenId no puede ser nulo");
        }
        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDateTime.now();
        }
    }
}
