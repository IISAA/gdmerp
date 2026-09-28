package pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DetalleOrdenRequest(
    @NotNull(message = "El ID de la materia prima es obligatorio") Long materiaPrimaId,
    @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad requerida debe ser al menos 1") Integer cantidadRequerida
) {}