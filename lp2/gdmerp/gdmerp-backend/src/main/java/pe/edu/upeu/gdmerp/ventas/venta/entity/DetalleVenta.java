package pe.edu.upeu.gdmerp.ventas.venta.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "DETALLE_VENTAS", schema = "BOM_VENTAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_VENTA", nullable = false)
    private Venta venta;

    @Column(name = "ID_PRODUCTO", nullable = false)
    private Long productoId;

    @Column(name = "ID_LOTE")
    private Long loteId;

    @Column(name = "NOMBRE_PRODUCTO", nullable = false, length = 120)
    private String nombreProducto;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}
