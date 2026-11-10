package pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity.MetodoPago;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCajaRequest {

    @NotNull(message = "El tipo de movimiento (INGRESO/EGRESO) es obligatorio")
    private TipoFinanciero tipo;

    @NotNull(message = "El método de pago es obligatorio")
    private MetodoPago metodoPago;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @NotBlank(message = "La descripción del movimiento es obligatoria")
    private String descripcion;

    private String referenciaExterna;

    @NotNull(message = "La categoría financiera es obligatoria")
    private Long categoriaFinancieraId;
}
