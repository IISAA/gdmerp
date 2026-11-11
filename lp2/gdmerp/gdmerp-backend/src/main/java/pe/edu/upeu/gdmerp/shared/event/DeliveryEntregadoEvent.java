package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de integración emitido por Delivery al liquidar una entrega contra pago.
 */
public record DeliveryEntregadoEvent(
        Long deliveryId,
        Long ventaId,
        String codigo,
        BigDecimal montoTotal,
        String metodoPago,
        LocalDateTime fechaEntrega
) {}
