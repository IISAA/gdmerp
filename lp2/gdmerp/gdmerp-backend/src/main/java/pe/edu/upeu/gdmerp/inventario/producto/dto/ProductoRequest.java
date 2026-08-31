package pe.edu.upeu.gdmerp.inventario.producto.dto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotNull(message = "El precio es obligatorio") @Min(value = 0) BigDecimal precio,
    @NotNull(message = "El stock es obligatorio") @Min(value = 0) Integer stock,
    @NotNull(message = "El ID del almacén es obligatorio") Long almacenId,
    @NotNull(message = "El ID de la categoría es obligatorio") Long categoriaId
) {}