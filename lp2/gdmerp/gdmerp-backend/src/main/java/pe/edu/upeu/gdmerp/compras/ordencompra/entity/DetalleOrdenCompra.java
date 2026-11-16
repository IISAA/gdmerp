package pe.edu.upeu.gdmerp.compras.ordencompra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad interna del Agregado Orden de Compra.
 * Gestiona exclusivamente los renglones comerciales acordados con el proveedor.
 *
 * MAPEO DE BASE DE DATOS ORACLE:
 * - Tabla: DETALLE_ORDEN_COMPRA en esquema GDM_COMPRAS.
 * - Secuencia: SEQ_DETALLE_ORDEN_COMPRA.
 *
 * REGLAS DE DOMINIO (BOUNDED CONTEXT COMPRAS):
 * - SOLO almacena productoId (Soft Link numérico), cantidadSolicitada, precioUnitarioPactado y subtotalLinea.
 * - ELIMINADOS por completo: lote_proveedor, fecha_vencimiento, factor_conversion (responsabilidad de Inventario).
 */
@Entity
@Table(name = "DETALLE_ORDEN_COMPRA", schema = "GDM_COMPRAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetalleOrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_detalle_orden_compra")
    @SequenceGenerator(
            name = "seq_detalle_orden_compra",
            sequenceName = "SEQ_DETALLE_ORDEN_COMPRA",
            schema = "GDM_COMPRAS",
            allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDEN_COMPRA_ID", nullable = false)
    private OrdenCompra ordenCompra;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "CANTIDAD_SOLICITADA", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidadSolicitada;

    @Column(name = "PRECIO_UNITARIO_PACTADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioPactado;

    @Column(name = "SUBTOTAL_LINEA", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalLinea;

    @PrePersist
    @PreUpdate
    public void calcularSubtotal() {
        if (cantidadSolicitada != null && precioUnitarioPactado != null) {
            this.subtotalLinea = cantidadSolicitada.multiply(precioUnitarioPactado);
        } else {
            this.subtotalLinea = BigDecimal.ZERO;
        }
    }

    // --- Métodos de compatibilidad y conveniencia ---
    public BigDecimal getPrecioPactado() {
        return this.precioUnitarioPactado;
    }

    public BigDecimal getSubtotal() {
        return this.subtotalLinea;
    }
}
