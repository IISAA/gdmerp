package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class RegistrarAbonoRequest {

    @NotNull(message = "El monto del abono es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto del abono debe ser mayor a cero")
    private BigDecimal monto;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    private Long sesionCajaId;

    private String nota;
}
