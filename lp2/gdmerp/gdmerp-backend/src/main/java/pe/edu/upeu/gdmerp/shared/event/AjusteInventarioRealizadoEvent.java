package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;

public record AjusteInventarioRealizadoEvent(
        Long ajusteId,
        Long productoId,
        BigDecimal diferencia,
        String motivoAjuste
) {
}
