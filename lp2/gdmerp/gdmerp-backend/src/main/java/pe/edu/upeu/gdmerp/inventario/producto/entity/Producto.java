package pe.edu.upeu.gdmerp.inventario.producto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCTOS", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String unidadMedida;

    @Column(name = "COSTO_PROMEDIO", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoPromedio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false)
    private TipoProducto tipoProducto = TipoProducto.MATERIA_PRIMA;

    // Precisión decimal de 4 dígitos para conteo de masa y volumen alimentario (kg, g, L, ml)
    @Column(name = "STOCK_TOTAL", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockTotal = BigDecimal.ZERO;

    @Column(name = "STOCK_MINIMO", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // Control de concurrencia optimista
    @Version
    @Column(name = "VERSION")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALMACEN_ID", nullable = false)
    private Almacen almacen;

    // Relación con los lotes para aplicar FEFO
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lote> lotes = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORIA_ID", nullable = false)
    private Categoria categoria;

    // Alias de compatibilidad (stockActual / stockFisico)
    public BigDecimal getStockActual() {
        return this.stockTotal;
    }

    public void setStockActual(BigDecimal stockActual) {
        setStockTotal(stockActual);
    }

    public BigDecimal getStockFisico() {
        return this.stockTotal;
    }

    public void setStockFisico(BigDecimal stockFisico) {
        setStockTotal(stockFisico);
    }

    // Sobrecargas de compatibilidad
    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal != null ? BigDecimal.valueOf(stockTotal) : BigDecimal.ZERO;
    }

    public void setStockTotal(Double stockTotal) {
        this.stockTotal = stockTotal != null ? BigDecimal.valueOf(stockTotal) : BigDecimal.ZERO;
    }

    public void setStockTotal(BigDecimal stockTotal) {
        this.stockTotal = stockTotal != null ? stockTotal : BigDecimal.ZERO;
    }

    public void setStockMinimo(Integer stockMinimo) {
        this.stockMinimo = stockMinimo != null ? BigDecimal.valueOf(stockMinimo) : BigDecimal.ZERO;
    }

    public void setStockMinimo(Double stockMinimo) {
        this.stockMinimo = stockMinimo != null ? BigDecimal.valueOf(stockMinimo) : BigDecimal.ZERO;
    }

    public void setStockMinimo(BigDecimal stockMinimo) {
        this.stockMinimo = stockMinimo != null ? stockMinimo : BigDecimal.ZERO;
    }

    public void setCostoPromedio(Double costoPromedio) {
        this.costoPromedio = costoPromedio != null ? BigDecimal.valueOf(costoPromedio) : null;
    }

    public void setCostoPromedio(BigDecimal costoPromedio) {
        this.costoPromedio = costoPromedio;
    }
}
