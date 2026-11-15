package pe.edu.upeu.gdmerp.catalogo.producto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.catalogo.categoria.dto.CategoriaResumen;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoResponse {
    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private BigDecimal precio;
    private BigDecimal costoBase;
    private TipoProducto tipoProducto;
    private BigDecimal stockMinimo;
    private Integer stock;
    private String imagenUrl;
    private Boolean activo;
    private CategoriaResumen categoria;

    public BigDecimal getCostoPromedio() {
        return costoBase;
    }
}
