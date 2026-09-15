package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductoRequest(
    @NotBlank(message = "El SKU es obligatorio")
    @Size(max = 50, message = "El SKU no puede superar 50 caracteres")
    String sku,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
    String nombre,

    @NotBlank(message = "La unidad de medida es obligatoria")
    @Size(max = 20, message = "La unidad de medida no puede superar 20 caracteres")
    String unidadMedida,

    @NotNull(message = "El costo es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
    BigDecimal costoPromedio,

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    Integer stockMinimo,

    @NotNull(message = "El ID del almacén es obligatorio")
    Long almacenId,

    @NotNull(message = "El ID de la categoría es obligatorio")
    Long categoriaId
) {}