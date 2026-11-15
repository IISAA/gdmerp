package pe.edu.upeu.gdmerp.catalogo.producto.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;

import java.math.BigDecimal;
import java.util.UUID;

@Entity(name = "CatalogoProducto")
@Table(
        name = "PRODUCTOS",
        schema = "GDM_CATALOGO",
        uniqueConstraints = @UniqueConstraint(name = "UK_SKU", columnNames = "SKU")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SKU", nullable = false, length = 50)
    private String sku;

    @Column(name = "NOMBRE", nullable = false, length = 120)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;

    @Column(name = "UNIDAD_MEDIDA", nullable = false, length = 30)
    @Builder.Default
    private String unidadMedida = "UND";

    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "COSTO_BASE", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal costoBase = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PRODUCTO", nullable = false, length = 25)
    @Builder.Default
    private TipoProducto tipoProducto = TipoProducto.TERMINADO;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "STOCK", nullable = false)
    @Builder.Default
    private Integer stock = 0;

    @Column(name = "IMAGEN_URL", length = 255)
    private String imagenUrl;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = Boolean.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA")
    private Categoria categoria;

    @PrePersist
    void prePersistDefaults() {
        if (sku == null || sku.isBlank()) {
            sku = "CAT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            sku = sku.trim().toUpperCase();
        }
        if (unidadMedida == null || unidadMedida.isBlank()) {
            unidadMedida = "UND";
        }
        if (precio == null) {
            precio = BigDecimal.ZERO;
        }
        if (costoBase == null) {
            costoBase = BigDecimal.ZERO;
        }
        if (tipoProducto == null) {
            tipoProducto = TipoProducto.TERMINADO;
        }
        if (stockMinimo == null) {
            stockMinimo = BigDecimal.ZERO;
        }
        if (stock == null) {
            stock = 0;
        }
        if (activo == null) {
            activo = Boolean.TRUE;
        }
    }

    @PreUpdate
    void preUpdateNormalize() {
        if (sku != null && !sku.isBlank()) {
            sku = sku.trim().toUpperCase();
        }
    }
}
