package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "DETALLES_ORDEN_PRODUCCION", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class DetalleOrdenProduccion implements InsumoDetalle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDEN_ID", nullable = false)
    private OrdenProduccion ordenProduccion;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long materiaPrimaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCTO_ID", insertable = false, updatable = false)
    private Producto insumo;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "CANTIDAD_REQUERIDA", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadRequerida;

    // Copia del costo unitario del insumo al momento del consumo (histórico).
    @Column(name = "COSTO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    // Métodos alias para soporte de cantidadConsumida
    public BigDecimal getCantidadConsumida() {
        return this.cantidadRequerida;
    }

    public void setCantidadConsumida(Integer cantidad) {
        setCantidadRequerida(cantidad);
    }

    public void setCantidadConsumida(Double cantidad) {
        setCantidadRequerida(cantidad);
    }

    public void setCantidadConsumida(BigDecimal cantidad) {
        setCantidadRequerida(cantidad);
    }

    public void setCantidadRequerida(Integer cantidad) {
        this.cantidadRequerida = cantidad != null ? BigDecimal.valueOf(cantidad) : null;
    }

    public void setCantidadRequerida(Double cantidad) {
        this.cantidadRequerida = cantidad != null ? BigDecimal.valueOf(cantidad) : null;
    }

    public void setCantidadRequerida(BigDecimal cantidad) {
        this.cantidadRequerida = cantidad;
    }

    // Cálculo de total por línea: cantidad requerida x costo unitario copiado.
    public BigDecimal getSubtotal() {
        if (costoUnitario == null || cantidadRequerida == null) return BigDecimal.ZERO;
        return costoUnitario.multiply(cantidadRequerida).setScale(2, RoundingMode.HALF_UP);
    }
}
