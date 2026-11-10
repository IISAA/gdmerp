package pe.edu.upeu.gdmerp.ventas.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DeliveryEntregadoEvent(
    Long deliveryId,
    Long ventaId,
    String codigo,
    BigDecimal montoTotal,
    String metodoPago,
    LocalDateTime fechaEntrega
) {}