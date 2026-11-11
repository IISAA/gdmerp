package pe.edu.upeu.gdmerp.shared.event;

import java.time.LocalDateTime;

/**
 * Evento de integración emitido por el módulo Compras al anular un comprobante.
 */
public record CompraAnuladaEvent(
        Long compraId,
        String numeroComprobante,
        LocalDateTime fechaAnulacion
) {}
