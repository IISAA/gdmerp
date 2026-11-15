package pe.edu.upeu.gdmerp.inventario.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;

import java.math.BigDecimal;

@Entity
@Table(
        name = "STOCK_INVENTARIO",
        schema = "GDM_INVENTARIO",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_STOCK_PRODUCTO_ALMACEN",
                columnNames = {"PRODUCTO_ID", "ALMACEN_ID"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class StockInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALMACEN_ID")
    private Almacen almacen;

    @Column(name = "CATEGORIA_ID")
    private Long categoriaId;

    @Column(name = "STOCK_ACTUAL", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "STOCK_MAXIMO", precision = 12, scale = 4)
    private BigDecimal stockMaximo;

    @Column(name = "COSTO_PROMEDIO", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoPromedio = BigDecimal.ZERO;

    @Version
    @Column(name = "VERSION")
    private Long version;

    public BigDecimal obtenerStockSeguro() {
        return stockActual != null ? stockActual : BigDecimal.ZERO;
    }

    public BigDecimal obtenerStockMinimoSeguro() {
        return stockMinimo != null ? stockMinimo : BigDecimal.ZERO;
    }

    public BigDecimal obtenerCostoPromedioSeguro() {
        return costoPromedio != null ? costoPromedio : BigDecimal.ZERO;
    }
}
