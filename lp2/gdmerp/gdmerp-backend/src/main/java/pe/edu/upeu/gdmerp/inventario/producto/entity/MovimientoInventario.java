package pe.edu.upeu.gdmerp.inventario.producto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de Kardex / Movimiento de Inventario con soporte de precisión decimal (12, 4).
 */
@Entity
@Table(name = "MOVIMIENTOS_INVENTARIO", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCTO_ID", nullable = false)
    private Producto producto;

    @Column(name = "TIPO_MOVIMIENTO", nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(name = "CANTIDAD", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "STOCK_ANTERIOR", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockAnterior;

    @Column(name = "STOCK_RESULTANTE", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockResultante;

    @Column(name = "COSTO_UNITARIO", precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    @Column(name = "FECHA_MOVIMIENTO", nullable = false)
    private LocalDateTime fechaMovimiento = LocalDateTime.now();

    @Column(name = "REFERENCIA", length = 120)
    private String referencia;

    public MovimientoInventario(Producto producto, String tipoMovimiento, BigDecimal cantidad,
                                BigDecimal stockAnterior, BigDecimal stockResultante,
                                BigDecimal costoUnitario, String referencia) {
        this.producto = producto;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockResultante;
        this.costoUnitario = costoUnitario;
        this.fechaMovimiento = LocalDateTime.now();
        this.referencia = referencia;
    }
}
