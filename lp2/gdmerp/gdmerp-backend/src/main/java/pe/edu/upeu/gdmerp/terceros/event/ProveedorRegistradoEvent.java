package pe.edu.upeu.gdmerp.terceros.event;

public record ProveedorRegistradoEvent(
        String ruc,
        String razonSocial,
        String telefono,
        String email,
        String direccion
) {
}
