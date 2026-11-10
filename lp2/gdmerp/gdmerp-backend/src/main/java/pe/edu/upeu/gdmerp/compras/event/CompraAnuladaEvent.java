package pe.edu.upeu.gdmerp.compras.event;

import java.time.LocalDateTime;

public record CompraAnuladaEvent(
    Long compraId,
    String numeroComprobante,
    LocalDateTime fechaAnulacion
) {}
