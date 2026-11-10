package pe.edu.upeu.gdmerp.ventas.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaCreadaEvent(
    Long ventaId,
    String numeroTicket,
    String clienteNombre,
    String vendedorId,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal total,
    Long sesionCajaId,
    String metodoPago,
    LocalDateTime fecha,
    List<ItemVentaEvent> items
) {
    public record ItemVentaEvent(
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        Long loteId
    ) {}
}