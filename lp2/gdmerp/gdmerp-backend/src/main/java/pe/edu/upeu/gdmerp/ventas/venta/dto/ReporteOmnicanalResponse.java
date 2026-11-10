package pe.edu.upeu.gdmerp.ventas.venta.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteOmnicanalResponse {
    private BigDecimal totalVentasOmnicanal;
    private Long totalTransacciones;
    private List<CanalVentaResumenDto> canales;
    private List<ProductoRentabilidadDto> rankingProductos;
}
