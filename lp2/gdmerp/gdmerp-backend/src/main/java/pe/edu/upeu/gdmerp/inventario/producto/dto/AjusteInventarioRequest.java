package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AjusteInventarioRequest(
        @NotNull(message = "La diferencia de stock es obligatoria")
        BigDecimal diferencia,
        @NotBlank(message = "El motivo del ajuste o merma es obligatorio")
        String motivoAjuste
) {
}
