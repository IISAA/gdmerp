package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de integración emitido por Compras al recepcionar mercadería física.
 * Inventario debe procesarlo en fase {@code BEFORE_COMMIT} para garantizar atomicidad:
 * cualquier falla al ingresar lotes o actualizar stock revierte la transacción de Compras.
 */
public record RecepcionCompraEvent(
        Long compraId,
        String numeroComprobante,
        LocalDateTime fechaRecepcion,
        List<ItemRecepcion> items
) {
    public record ItemRecepcion(
            Long productoId,
            BigDecimal cantidadRecibida,
            LocalDate fechaVencimiento
    ) {}
}
