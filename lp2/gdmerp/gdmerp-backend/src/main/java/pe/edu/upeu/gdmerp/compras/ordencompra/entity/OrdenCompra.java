package pe.edu.upeu.gdmerp.compras.ordencompra.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregado Raíz del Bounded Context COMPRAS (Negociación Comercial).
 * Representa exclusivamente el acuerdo comercial y contractual con el proveedor.
 *
 * MAPEO DE BASE DE DATOS ORACLE:
 * - Tabla: ORDEN_COMPRA en esquema GDM_COMPRAS.
 * - Secuencia: SEQ_ORDEN_COMPRA.
 *
 * REGLAS DE DOMINIO Y DISEÑO DDD:
 * - ELIMINADOS de forma definitiva: serie_comprobante, numero_comprobante, fecha_emision del comprobante
 *   (responsabilidad exclusiva del Bounded Context de Finanzas al registrar la Factura/XML).
 * - Soft Link hacia Terceros: proveedorId numérico puro (sin @ManyToOne hacia Proveedor).
 * - Cálculos de negociación: subtotalEstimado, impuestosEstimados, costoFleteEstimado y totalEstimado.
 */
@Entity
@Table(name = "ORDEN_COMPRA", schema = "GDM_COMPRAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompra {

    private static final BigDecimal TASA_IGV_ESTIMADA = new BigDecimal("0.18");

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_orden_compra")
    @SequenceGenerator(
            name = "seq_orden_compra",
            sequenceName = "SEQ_ORDEN_COMPRA",
            schema = "GDM_COMPRAS",
            allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "PROVEEDOR_ID", nullable = false)
    private Long proveedorId;

    @Column(name = "CODIGO_ORDEN", nullable = false, unique = true, length = 50)
    private String codigoOrden;

    @Column(name = "REFERENCIA_INTERNA", length = 50)
    private String referenciaInterna;

    @Column(name = "FECHA_ORDEN", nullable = false)
    private LocalDate fechaOrden;

    @Column(name = "FECHA_ENTREGA_ESPERADA")
    private LocalDate fechaEntregaEsperada;

    @Column(name = "CONDICION_PAGO", length = 50)
    private String condicionPago;

    @Column(name = "COSTO_FLETE_ESTIMADO", precision = 12, scale = 2)
    private BigDecimal costoFleteEstimado;

    @Column(name = "SUBTOTAL_ESTIMADO", precision = 12, scale = 2)
    private BigDecimal subtotalEstimado;

    @Column(name = "IMPUESTOS_ESTIMADOS", precision = 12, scale = 2)
    private BigDecimal impuestosEstimados;

    @Column(name = "TOTAL_ESTIMADO", precision = 12, scale = 2)
    private BigDecimal totalEstimado;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoCompra estado;

    @Column(name = "NOTAS", length = 500)
    private String notas;

    @Column(name = "FECHA_RECEPCION")
    private LocalDateTime fechaRecepcion;

    // --- Auditoría Mandatoria ---
    @Column(name = "FECHA_CREACION", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "USUARIO_CREACION", nullable = false, updatable = false, length = 50)
    private String usuarioCreacion;

    @Column(name = "FECHA_MODIFICACION")
    private LocalDateTime fechaModificacion;

    @Column(name = "USUARIO_MODIFICACION", length = 50)
    private String usuarioModificacion;

    @Builder.Default
    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrdenCompra> detalles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.usuarioCreacion == null || this.usuarioCreacion.isBlank()) {
            this.usuarioCreacion = "SYSTEM";
        }
        if (this.estado == null) {
            this.estado = EstadoCompra.BORRADOR;
        }
        if (this.fechaOrden == null) {
            this.fechaOrden = LocalDate.now();
        }
        recalcularTotalesEstimados();
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaModificacion = LocalDateTime.now();
        recalcularTotalesEstimados();
    }

    // --- Métodos de Negocio del Agregado (DDD) ---

    public void agregarDetalle(Long productoId, BigDecimal cantidad, BigDecimal precioUnitario) {
        if (this.estado != EstadoCompra.BORRADOR) {
            throw new BusinessException("Solo se pueden agregar o modificar líneas en una orden en estado BORRADOR.");
        }
        if (productoId == null) {
            throw new BusinessException("El productoId es obligatorio para cada línea.");
        }
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad solicitada debe ser estrictamente mayor a cero.");
        }
        if (precioUnitario == null || precioUnitario.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El precio unitario pactado no puede ser negativo.");
        }

        DetalleOrdenCompra detalle = DetalleOrdenCompra.builder()
                .ordenCompra(this)
                .productoId(productoId)
                .cantidadSolicitada(cantidad)
                .precioUnitarioPactado(precioUnitario)
                .subtotalLinea(cantidad.multiply(precioUnitario).setScale(2, RoundingMode.HALF_UP))
                .build();

        this.detalles.add(detalle);
        recalcularTotalesEstimados();
    }

    public void recalcularTotalesEstimados() {
        if (this.detalles == null || this.detalles.isEmpty()) {
            this.subtotalEstimado = BigDecimal.ZERO;
            this.impuestosEstimados = BigDecimal.ZERO;
            this.totalEstimado = (this.costoFleteEstimado != null) ? this.costoFleteEstimado : BigDecimal.ZERO;
            return;
        }

        this.subtotalEstimado = this.detalles.stream()
                .map(d -> {
                    if (d.getSubtotalLinea() == null) {
                        d.calcularSubtotal();
                    }
                    return d.getSubtotalLinea();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        this.impuestosEstimados = this.subtotalEstimado.multiply(TASA_IGV_ESTIMADA).setScale(2, RoundingMode.HALF_UP);

        BigDecimal flete = (this.costoFleteEstimado != null) ? this.costoFleteEstimado : BigDecimal.ZERO;
        this.totalEstimado = this.subtotalEstimado.add(this.impuestosEstimados).add(flete).setScale(2, RoundingMode.HALF_UP);
    }

    public void confirmar(String usuario) {
        if (this.estado != EstadoCompra.BORRADOR) {
            throw new BusinessException("Transición inválida: No se puede confirmar una orden en estado "
                    + this.estado + ". Solo las órdenes en estado BORRADOR pueden ser confirmadas.");
        }
        if (this.detalles == null || this.detalles.isEmpty()) {
            throw new BusinessException("No se puede confirmar una orden de compra sin líneas de detalle.");
        }
        this.estado = EstadoCompra.CONFIRMADA;
        this.fechaModificacion = LocalDateTime.now();
        this.usuarioModificacion = usuario;
    }

    public void marcarRecibida(LocalDateTime fechaRecepcion, String usuario) {
        if (this.estado != EstadoCompra.CONFIRMADA) {
            throw new BusinessException("Transición inválida: La orden debe estar en estado CONFIRMADA para marcarse como RECIBIDA. Estado actual: "
                    + this.estado);
        }
        this.estado = EstadoCompra.RECIBIDA;
        this.fechaRecepcion = (fechaRecepcion != null) ? fechaRecepcion : LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        this.usuarioModificacion = usuario;
    }

    public void marcarFacturada(String comprobanteFisico, String usuario) {
        if (this.estado != EstadoCompra.RECIBIDA) {
            throw new BusinessException("Transición inválida (3-Way Matching): La orden debe haber sido RECIBIDA físicamente antes de ser FACTURADA. Estado actual: "
                    + this.estado);
        }
        this.estado = EstadoCompra.FACTURADA;
        if (comprobanteFisico != null && !comprobanteFisico.isBlank()) {
            this.notas = (this.notas != null ? this.notas + " | " : "") + "Factura Proveedor: " + comprobanteFisico;
        }
        this.fechaModificacion = LocalDateTime.now();
        this.usuarioModificacion = usuario;
    }

    public void cancelar(String motivo, String usuario) {
        if (this.estado == EstadoCompra.FACTURADA || this.estado == EstadoCompra.RECIBIDA) {
            throw new BusinessException("No se puede cancelar una orden de compra que ya ha sido " + this.estado + ".");
        }
        if (this.estado == EstadoCompra.CANCELADA || this.estado == EstadoCompra.ANULADA) {
            throw new BusinessException("La orden de compra ya se encuentra cancelada.");
        }
        this.estado = EstadoCompra.CANCELADA;
        this.notas = (this.notas != null ? this.notas + " | " : "") + "Motivo cancelación: " + motivo;
        this.fechaModificacion = LocalDateTime.now();
        this.usuarioModificacion = usuario;
    }

    @Deprecated
    public void anular(String motivo, String usuario) {
        cancelar(motivo, usuario);
    }

    // --- Métodos de compatibilidad y conveniencia con servicios existentes ---
    public String getCodigo() {
        return this.codigoOrden;
    }

    public void setCodigo(String codigo) {
        this.codigoOrden = codigo;
    }

    public LocalDate getFechaEsperada() {
        return this.fechaEntregaEsperada;
    }

    public void setFechaEsperada(LocalDate fechaEsperada) {
        this.fechaEntregaEsperada = fechaEsperada;
    }

    public BigDecimal getMontoTotal() {
        return this.totalEstimado;
    }
}
