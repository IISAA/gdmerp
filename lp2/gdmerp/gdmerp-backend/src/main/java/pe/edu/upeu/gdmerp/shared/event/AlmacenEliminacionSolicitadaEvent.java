package pe.edu.upeu.gdmerp.shared.event;

/**
 * Evento de integración emitido antes de eliminar un Almacén para validar en Inventario
 * que no existan productos asociados sin acoplar AlmacenServiceImpl a ProductoRepository.
 */
public record AlmacenEliminacionSolicitadaEvent(
        Long almacenId,
        String nombreAlmacen
) {}
