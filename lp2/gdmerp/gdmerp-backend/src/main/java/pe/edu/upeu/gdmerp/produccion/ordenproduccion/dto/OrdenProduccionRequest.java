package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrdenProduccionRequest(
    @NotBlank(message = "El producto es obligatorio") String producto,
    @NotNull(message = "La cantidad es obligatoria") @Min(value = 1) Integer cantidadPlanificada,
    @NotBlank(message = "El estado es obligatorio") String estado
) {}