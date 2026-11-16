package pe.edu.upeu.gdmerp.shared.event;

import java.math.BigDecimal;

/**
 * DTO inmutable para transportar ítems de insumos/productos en eventos de compras.
 * Representa la cantidad solicitada y el precio pactado sin mezclar datos de almacén (lotes) ni fiscales (series).
 */
public record InsumoEventDto(
        Long productoId,
        BigDecimal cantidadSolicitada,
        BigDecimal precioPactado
) {
    public InsumoEventDto {
        if (productoId == null) {
            throw new IllegalArgumentException("productoId no puede ser nulo");
        }
        if (cantidadSolicitada == null || cantidadSolicitada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("cantidadSolicitada debe ser mayor a cero");
        }
        if (precioPactado == null || precioPactado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("precioPactado no puede ser negativo");
        }
    }
}
