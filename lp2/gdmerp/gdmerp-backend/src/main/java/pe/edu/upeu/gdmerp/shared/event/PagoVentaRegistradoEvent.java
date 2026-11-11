package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;

public record PagoVentaRegistradoEvent(
        Long ventaId,
        Long transaccionFinancieraId,
        BigDecimal montoPagado
) {
}
