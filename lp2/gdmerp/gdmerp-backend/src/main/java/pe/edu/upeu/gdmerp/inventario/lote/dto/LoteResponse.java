package pe.edu.upeu.gdmerp.inventario.lote.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoteResponse(
        Long id,
        String numeroLote,
        Long productoId,
        String productoNombre,
        BigDecimal cantidadActual,
        LocalDate fechaProduccion,
        LocalDate fechaVencimiento) {
}