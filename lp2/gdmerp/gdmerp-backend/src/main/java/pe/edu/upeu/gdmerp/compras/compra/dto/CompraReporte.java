package pe.edu.upeu.gdmerp.compras.compra.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraReporte {
    private Long totalCompras;
    private BigDecimal montoTotalComprado;
    private BigDecimal montoTotalPagado;
    private BigDecimal saldoPendienteTotal;
    private BigDecimal ticketPromedioCompra;
    private List<CompraResponse> compras;
}
