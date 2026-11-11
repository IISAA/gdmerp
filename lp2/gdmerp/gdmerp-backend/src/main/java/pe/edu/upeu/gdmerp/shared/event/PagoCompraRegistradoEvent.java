package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento de integración emitido por Compras al amortizar o liquidar el pago a un proveedor.
 */
public record PagoCompraRegistradoEvent(
        Long compraId,
        String numeroComprobante,
        String proveedorRazonSocial,
        BigDecimal montoPagado,
        String metodoPago,
        boolean liquidadaTotalmente,
        LocalDate fechaPago
) {}
