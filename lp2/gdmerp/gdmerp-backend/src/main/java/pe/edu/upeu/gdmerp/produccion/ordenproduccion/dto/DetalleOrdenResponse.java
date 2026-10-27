package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;

import java.math.BigDecimal;

public record DetalleOrdenResponse(
    Long id,
    Long materiaPrimaId,
    String nombreMateriaPrima,
    BigDecimal cantidadRequerida,
    BigDecimal costoUnitario,
    BigDecimal subtotal
) {
    public Long insumoId() {
        return materiaPrimaId;
    }

    public BigDecimal cantidad() {
        return cantidadRequerida;
    }
}
