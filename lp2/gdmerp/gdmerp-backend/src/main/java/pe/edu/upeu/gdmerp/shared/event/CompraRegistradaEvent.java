package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de integración emitido por el módulo Compras al registrar una compra de insumos.
 */
public record CompraRegistradaEvent(
        Long compraId,
        String numeroComprobante,
        String proveedorRazonSocial,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total,
        LocalDate fechaEmision,
        LocalDateTime fechaRegistro,
        List<ItemCompraEvent> items
) {
    public record ItemCompraEvent(
            Long insumoId,
            String nombreInsumo,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal,
            String loteProveedor,
            LocalDate fechaVencimiento
    ) {}
}
