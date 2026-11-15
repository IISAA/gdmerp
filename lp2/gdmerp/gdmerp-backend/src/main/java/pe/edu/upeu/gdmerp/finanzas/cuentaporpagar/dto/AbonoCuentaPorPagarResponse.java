package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto;

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
public class AbonoCuentaPorPagarResponse {
    private Long id;
    private Long cuentaPorPagarId;
    private Long sesionCajaId;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private LocalDateTime fecha;
    private String numeroOperacion;
    private String nota;
}
