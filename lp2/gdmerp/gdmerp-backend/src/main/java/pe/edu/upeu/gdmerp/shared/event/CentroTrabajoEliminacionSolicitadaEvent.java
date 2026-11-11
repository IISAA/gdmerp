package pe.edu.upeu.gdmerp.shared.event;

/**
 * Evento de integración emitido antes de eliminar un Centro de Trabajo para validar
 * la ausencia de órdenes asociadas sin acoplar CentroTrabajoServiceImpl a OrdenProduccionRepository.
 */
public record CentroTrabajoEliminacionSolicitadaEvent(
        Long centroTrabajoId,
        String nombreCentroTrabajo
) {}
