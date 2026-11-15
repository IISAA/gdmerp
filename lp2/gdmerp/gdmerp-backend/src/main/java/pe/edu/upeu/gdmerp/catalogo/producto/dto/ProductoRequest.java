package pe.edu.upeu.gdmerp.catalogo.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequest {

    @Size(max = 50, message = "El SKU no puede exceder 50 caracteres")
    private String sku;

    @NotBlank(message = "El nombre del producto no puede estar vacío")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private String descripcion;

    @Size(max = 30, message = "La unidad de medida no puede exceder 30 caracteres")
    @Builder.Default
    private String unidadMedida = "UND";

    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor o igual a 0")
    private BigDecimal precio;

    @PositiveOrZero(message = "El costo base no puede ser negativo")
    private BigDecimal costoBase;

    private BigDecimal costoPromedio;

    public BigDecimal getCostoBase() {
        if (costoBase != null) {
            return costoBase;
        }
        return costoPromedio;
    }

    public void setCostoPromedio(BigDecimal costoPromedio) {
        this.costoPromedio = costoPromedio;
        if (this.costoBase == null) {
            this.costoBase = costoPromedio;
        }
    }

    @Builder.Default
    private TipoProducto tipoProducto = TipoProducto.TERMINADO;

    @PositiveOrZero(message = "El stock mínimo debe ser mayor o igual a 0")
    @Builder.Default
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @PositiveOrZero(message = "El stock debe ser positivo o cero")
    @Builder.Default
    private Integer stock = 0;

    private String imagenUrl;

    private Long categoriaId;

    @Builder.Default
    private Boolean activo = Boolean.TRUE;

    public ProductoRequest(String nombre, BigDecimal precio, Integer stock, String imagenUrl, Long categoriaId) {
        this.nombre = nombre;
        this.precio = precio;
        this.stock = stock != null ? stock : 0;
        this.imagenUrl = imagenUrl;
        this.categoriaId = categoriaId;
        this.unidadMedida = "UND";
        this.costoBase = BigDecimal.ZERO;
        this.tipoProducto = TipoProducto.TERMINADO;
        this.stockMinimo = BigDecimal.ZERO;
        this.activo = Boolean.TRUE;
    }
}
