package pe.edu.upeu.gdmerp.ventas.venta.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVentaResponse {
    private Long id;
    private Long productoId;
    private Long loteId;
    private String nombreProducto;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
}
