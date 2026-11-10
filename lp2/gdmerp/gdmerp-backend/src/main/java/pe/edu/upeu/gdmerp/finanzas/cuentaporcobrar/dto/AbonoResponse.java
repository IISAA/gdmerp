package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbonoResponse {
    private Long id;
    private Long cuentaPorCobrarId;
    private Long sesionCajaId;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private LocalDateTime fecha;
    private String nota;
}
