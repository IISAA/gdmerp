package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
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
