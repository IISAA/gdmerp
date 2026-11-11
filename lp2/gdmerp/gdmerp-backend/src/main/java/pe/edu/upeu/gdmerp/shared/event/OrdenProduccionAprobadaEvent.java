package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;
import java.util.List;

public record OrdenProduccionAprobadaEvent(
        Long ordenId,
        Long centroTrabajoId,
        List<InsumoDto> insumosConsumidos
) {

    public OrdenProduccionAprobadaEvent {
        insumosConsumidos = insumosConsumidos == null ? List.of() : List.copyOf(insumosConsumidos);
    }

    public record InsumoDto(
            Long productoId,
            String nombreProducto,
            BigDecimal cantidadConsumida
    ) {
        public InsumoDto(Long productoId, BigDecimal cantidadConsumida) {
            this(productoId, null, cantidadConsumida);
        }
    }
}
