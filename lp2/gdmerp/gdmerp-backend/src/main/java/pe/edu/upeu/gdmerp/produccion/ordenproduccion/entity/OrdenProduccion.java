package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import org.hibernate.annotations.BatchSize;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORDENES_PRODUCCION", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class OrdenProduccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "PRODUCTO_TERMINADO_ID", nullable = false)
    private Long productoTerminadoId;

    @Column(name = "PRODUCTO", nullable = false, length = 120)
    private String producto;

    // Cantidad planificada con precisión decimal (escala = 4)
    @Column(name = "CANTIDAD_PLANIFICADA", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadPlanificada;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 30)
    private EstadoOrdenProduccion estado;

    @Column(name = "FECHA_PLANIFICADA_INICIO")
    private LocalDateTime fechaPlanificadaInicio;

    @Column(name = "FECHA_ESTIMADA_FIN")
    private LocalDateTime fechaEstimadaFin;

    @Column(name = "LOTE_GENERADO", length = 50)
    private String loteGenerado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CENTRO_TRABAJO_ID", nullable = false)
    private CentroTrabajo centroTrabajo;

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "ordenProduccion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleOrdenProduccion> detalles = new ArrayList<>();

    public void addDetalle(DetalleOrdenProduccion detalle) {
        detalles.add(detalle);
        detalle.setOrdenProduccion(this);
    }

    public BigDecimal getCantidad() {
        return this.cantidadPlanificada;
    }

    public void setCantidad(BigDecimal cantidad) {
        setCantidadPlanificada(cantidad);
    }

    public void setCantidad(Integer cantidad) {
        setCantidadPlanificada(cantidad);
    }

    public void setCantidad(Double cantidad) {
        setCantidadPlanificada(cantidad);
    }

    public void setCantidadPlanificada(Integer cantidad) {
        this.cantidadPlanificada = cantidad != null ? BigDecimal.valueOf(cantidad) : null;
    }

    public void setCantidadPlanificada(Double cantidad) {
        this.cantidadPlanificada = cantidad != null ? BigDecimal.valueOf(cantidad) : null;
    }

    public void setCantidadPlanificada(BigDecimal cantidad) {
        this.cantidadPlanificada = cantidad;
    }

    public String getLote() {
        return this.loteGenerado;
    }

    public EstadoOrden getEstadoOrden() {
        return EstadoOrden.from(this.estado);
    }

    public void setEstado(EstadoOrden estadoOrden) {
        this.estado = estadoOrden != null ? estadoOrden.toEstadoOrdenProduccion() : null;
    }

    public void setEstado(EstadoOrdenProduccion estado) {
        this.estado = estado;
    }

    public BigDecimal getTotalCostoInsumos() {
        if (detalles == null) {
            return BigDecimal.ZERO;
        }
        return detalles.stream()
                .map(detalle -> (detalle != null && detalle.getSubtotal() != null) ? detalle.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, (acumulado, subtotal) -> {
                    BigDecimal a = (acumulado != null) ? acumulado : BigDecimal.ZERO;
                    BigDecimal b = (subtotal != null) ? subtotal : BigDecimal.ZERO;
                    return a.add(b);
                });
    }
}
