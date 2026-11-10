package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarPresupuestoRequest {

    @NotNull(message = "El id del centro de costo es obligatorio")
    private Long centroCostoId;

    @NotNull(message = "El monto asignado es obligatorio")
    @DecimalMin(value = "0.01", message = "El presupuesto asignado debe ser mayor a 0")
    private BigDecimal montoPresupuesto;
}
