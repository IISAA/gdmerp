package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DetalleRendicionRequest {

    @NotBlank(message = "El concepto es obligatorio")
    private String concepto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @Positive(message = "El precio unitario debe ser positivo")
    private BigDecimal precioUnitario;

    private Long productoId;
    private Long centroCostoId;

    public DetalleRendicionRequest() {
    }

    public DetalleRendicionRequest(String concepto, Integer cantidad, BigDecimal precioUnitario, Long productoId) {
        this(concepto, cantidad, precioUnitario, productoId, null);
    }

    public DetalleRendicionRequest(String concepto, Integer cantidad, BigDecimal precioUnitario, Long productoId, Long centroCostoId) {
        this.concepto = concepto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.productoId = productoId;
        this.centroCostoId = centroCostoId;
    }
}
