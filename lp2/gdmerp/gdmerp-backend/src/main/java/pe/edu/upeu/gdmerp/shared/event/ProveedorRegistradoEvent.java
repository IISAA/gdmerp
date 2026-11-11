package pe.edu.upeu.gdmerp.shared.event;

/**
 * Evento de integración emitido por Terceros (CRM) al registrar o actualizar un proveedor.
 */
public record ProveedorRegistradoEvent(
        String ruc,
        String razonSocial,
        String telefono,
        String email,
        String direccion
) {}
