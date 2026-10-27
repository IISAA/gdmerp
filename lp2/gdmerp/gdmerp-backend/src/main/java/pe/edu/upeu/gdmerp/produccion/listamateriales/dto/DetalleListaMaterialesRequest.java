package pe.edu.upeu.gdmerp.produccion.listamateriales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DetalleListaMaterialesRequest(
    @NotNull(message = "El ID del insumo es obligatorio")
    Long insumoId,

    @NotNull(message = "La cantidad requerida es obligatoria")
    @DecimalMin(value = "0.0001", message = "La cantidad requerida debe ser mayor a 0")
    BigDecimal cantidadRequerida
) {}
