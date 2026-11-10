package pe.edu.upeu.gdmerp.ventas.delivery.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import pe.edu.upeu.gdmerp.ventas.delivery.entity.EstadoDelivery;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstadoDeliveryRequest {

    @NotNull(message = "El nuevo estado de delivery es obligatorio")
    private EstadoDelivery estado;

    private String observaciones;
}

