package pe.edu.upeu.gdmerp.inventario.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.gdmerp.inventario.producto.entity.TipoProducto;
import java.math.BigDecimal;

public record ProductoRequest(
    @Size(max = 30, message = "El SKU no puede superar 30 caracteres")
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
    @DecimalMin(value = "0.00", message = "El stock mínimo no puede ser negativo")
    BigDecimal stockMinimo,

    @DecimalMin(value = "0.00", message = "El stock actual no puede ser negativo")
    BigDecimal stockActual,

    @DecimalMin(value = "0.00", message = "El stock máximo no puede ser negativo")
    BigDecimal stockMaximo,

    @NotNull(message = "El ID del almacén es obligatorio")
    Long almacenId,

    @NotNull(message = "El ID de la categoría es obligatorio")
    Long categoriaId,

    TipoProducto tipoProducto
) {
    public ProductoRequest(String sku, String nombre, String unidadMedida, BigDecimal costoPromedio, BigDecimal stockMinimo, Long almacenId, Long categoriaId, TipoProducto tipoProducto) {
        this(sku, nombre, unidadMedida, costoPromedio, stockMinimo, BigDecimal.ZERO, null, almacenId, categoriaId, tipoProducto);
    }

    public ProductoRequest(String sku, String nombre, String unidadMedida, BigDecimal costoPromedio, BigDecimal stockMinimo, Long almacenId, Long categoriaId) {
        this(sku, nombre, unidadMedida, costoPromedio, stockMinimo, BigDecimal.ZERO, null, almacenId, categoriaId, TipoProducto.MATERIA_PRIMA);
    }

    public ProductoRequest(String sku, String nombre, String unidadMedida, BigDecimal costoPromedio, Integer stockMinimo, Long almacenId, Long categoriaId) {
        this(sku, nombre, unidadMedida, costoPromedio, stockMinimo != null ? BigDecimal.valueOf(stockMinimo) : BigDecimal.ZERO, BigDecimal.ZERO, null, almacenId, categoriaId, TipoProducto.MATERIA_PRIMA);
    }
}
