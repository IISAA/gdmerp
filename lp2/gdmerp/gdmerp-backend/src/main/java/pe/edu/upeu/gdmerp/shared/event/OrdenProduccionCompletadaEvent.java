package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;

public record OrdenProduccionCompletadaEvent(
        Long ordenId,
        Long productoTerminadoId,
        BigDecimal cantidadProducida,
        Long almacenDestinoId
) {
}
