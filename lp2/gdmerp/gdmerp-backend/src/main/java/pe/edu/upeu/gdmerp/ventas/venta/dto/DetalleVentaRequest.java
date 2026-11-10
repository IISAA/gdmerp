package pe.edu.upeu.gdmerp.ventas.venta.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVentaRequest {

    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;

    private Long loteId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
}
