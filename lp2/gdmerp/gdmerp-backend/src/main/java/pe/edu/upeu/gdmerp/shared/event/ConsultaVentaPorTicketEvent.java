package pe.edu.upeu.gdmerp.shared.event;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Evento de consulta síncrona desacoplada para resolver el ID de una Venta
 * a partir de su número de ticket sin acoplar Finanzas con el módulo Ventas.
 */
public record ConsultaVentaPorTicketEvent(
        String numeroTicket,
        AtomicReference<Long> ventaIdEncontrado
) {
    public ConsultaVentaPorTicketEvent(String numeroTicket) {
        this(numeroTicket, new AtomicReference<>());
    }
}
