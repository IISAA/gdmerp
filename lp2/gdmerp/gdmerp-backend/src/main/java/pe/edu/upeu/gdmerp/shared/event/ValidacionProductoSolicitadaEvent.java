package pe.edu.upeu.gdmerp.shared.event;

/**
 * Evento de integración emitido por módulos consumidores (ej. Finanzas) para validar
 * la existencia de un producto en Inventario sin inyectar ProductoService directamente.
 */
public record ValidacionProductoSolicitadaEvent(
        Long productoId,
        String moduloOrigen
) {}
