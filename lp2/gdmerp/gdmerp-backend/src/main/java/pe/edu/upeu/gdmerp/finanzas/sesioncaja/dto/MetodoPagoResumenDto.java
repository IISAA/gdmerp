package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetodoPagoResumenDto {
    private MetodoPago metodoPago;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal neto;
}
