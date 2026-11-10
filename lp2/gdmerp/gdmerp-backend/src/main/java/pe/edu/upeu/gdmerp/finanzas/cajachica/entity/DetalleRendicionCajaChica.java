package pe.edu.upeu.gdmerp.finanzas.cajachica.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "DETALLES_RENDICION_CAJA_CHICA")
@Getter
@Setter
@Builder
public class DetalleRendicionCajaChica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CONCEPTO", nullable = false, length = 200)
    private String concepto;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "SUBTOTAL", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "PRODUCTO_ID")
    private Long productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_RENDICION", nullable = false)
    private RendicionCajaChica rendicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CENTRO_COSTO")
    private CentroCosto centroCosto;

    public DetalleRendicionCajaChica() {
    }

    public DetalleRendicionCajaChica(Long id, String concepto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, Long productoId, RendicionCajaChica rendicion) {
        this(id, concepto, cantidad, precioUnitario, subtotal, productoId, rendicion, null);
    }

    public DetalleRendicionCajaChica(Long id, String concepto, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal, Long productoId, RendicionCajaChica rendicion, CentroCosto centroCosto) {
        this.id = id;
        this.concepto = concepto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.productoId = productoId;
        this.rendicion = rendicion;
        this.centroCosto = centroCosto;
    }
}
