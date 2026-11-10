package pe.edu.upeu.gdmerp.compras.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoCompraRegistradoEvent(
    Long compraId,
    String numeroComprobante,
    String proveedorRazonSocial,
    BigDecimal montoPagado,
    String metodoPago,
    boolean liquidadaTotalmente,
    LocalDate fechaPago
) {}
