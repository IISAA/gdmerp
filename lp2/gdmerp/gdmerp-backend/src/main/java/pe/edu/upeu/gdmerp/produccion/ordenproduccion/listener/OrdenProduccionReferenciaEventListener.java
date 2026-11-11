package pe.edu.upeu.gdmerp.produccion.ordenproduccion.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import pe.edu.upeu.gdmerp.shared.event.CentroTrabajoEliminacionSolicitadaEvent;

/**
 * Desacopla CentroTrabajoServiceImpl de OrdenProduccionRepository validando síncronamente dentro
 * de la transacción activa antes de ejecutar el borrado del centro de trabajo.
 */
@Component
@RequiredArgsConstructor
public class OrdenProduccionReferenciaEventListener {

    private final OrdenProduccionRepository ordenProduccionRepository;

    @EventListener
    public void onCentroTrabajoEliminacionSolicitada(CentroTrabajoEliminacionSolicitadaEvent event) {
        if (ordenProduccionRepository.existsByCentroTrabajoId(event.centroTrabajoId())) {
            throw new IllegalStateException(
                    "No se puede eliminar el centro de trabajo con id " + event.centroTrabajoId()
                            + " porque tiene órdenes de producción asociadas");
        }
    }
}
