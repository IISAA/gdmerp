package pe.edu.upeu.gdmerp.inventario.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MOVIMIENTOS_INVENTARIO", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "ALMACEN_ID")
    private Long almacenId;

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

    public MovimientoInventario(Long productoId,
                                Long almacenId,
                                String tipoMovimiento,
                                BigDecimal cantidad,
                                BigDecimal stockAnterior,
                                BigDecimal stockResultante,
                                BigDecimal costoUnitario,
                                String referencia) {
        this.productoId = productoId;
        this.almacenId = almacenId;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockResultante = stockResultante;
        this.costoUnitario = costoUnitario;
        this.referencia = referencia;
        this.fechaMovimiento = LocalDateTime.now();
    }
}
