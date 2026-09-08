package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductoRequest(
    @NotBlank(message = "El SKU es obligatorio") String sku,
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @NotBlank(message = "La unidad de medida es obligatoria") String unidadMedida,
    @NotNull(message = "El costo es obligatorio") @Min(0) BigDecimal costoPromedio,
    @NotNull(message = "El stock total es obligatorio") @Min(0) Integer stockTotal,
    @NotNull(message = "El stock mínimo es obligatorio") @Min(0) Integer stockMinimo,
    @NotNull(message = "El ID del almacén es obligatorio") Long almacenId
) {}