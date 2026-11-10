package pe.edu.upeu.gdmerp.ventas.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaAnuladaEvent(
    Long ventaId,
    String numeroTicket,
    String vendedorId,
    BigDecimal total,
    Long sesionCajaId,
    String metodoPago,
    LocalDateTime fechaAnulacion,
    List<ItemVentaAnuladaEvent> items
) {
    public record ItemVentaAnuladaEvent(
        Long productoId,
        Integer cantidad
    ) {}
}