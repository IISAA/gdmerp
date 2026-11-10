package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DetalleRendicionResponse {
    private Long id;
    private String concepto;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Long productoId;
    private Long centroCostoId;
    private String centroCostoCodigo;
    private String centroCostoNombre;

    public DetalleRendicionResponse() {
    }

    public DetalleRendicionResponse(Long id, String concepto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, Long productoId) {
        this(id, concepto, cantidad, precioUnitario, subtotal, productoId, null, null, null);
    }

    public DetalleRendicionResponse(Long id, String concepto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, Long productoId, Long centroCostoId, String centroCostoCodigo, String centroCostoNombre) {
        this.id = id;
        this.concepto = concepto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.productoId = productoId;
        this.centroCostoId = centroCostoId;
        this.centroCostoCodigo = centroCostoCodigo;
        this.centroCostoNombre = centroCostoNombre;
    }
}
