package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "DETALLES_ORDEN_PRODUCCION", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class DetalleOrdenProduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDEN_ID", nullable = false)
    private OrdenProduccion ordenProduccion;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "CANTIDAD_REQUERIDA", nullable = false)
    private Integer cantidadRequerida;

    // Copia del costo unitario del insumo al momento del consumo (histórico).
    @Column(name = "COSTO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    // Cálculo de total por línea: cantidad requerida x costo unitario copiado.
    public BigDecimal getSubtotal() {
        return costoUnitario.multiply(BigDecimal.valueOf(cantidadRequerida));
    }
}