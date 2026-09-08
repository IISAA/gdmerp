package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
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

    @Column(name = "CANTIDAD_PLANIFICADA", nullable = false)
    private Integer cantidadPlanificada;

    @Column(name = "ESTADO", nullable = false, length = 30)
    private String estado;

    @Column(name = "LOTE_GENERADO", length = 50)
    private String loteGenerado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CENTRO_TRABAJO_ID", nullable = false)
    private CentroTrabajo centroTrabajo;

    @OneToMany(mappedBy = "ordenProduccion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleOrdenProduccion> detalles = new ArrayList<>();

    public void addDetalle(DetalleOrdenProduccion detalle) {
        detalles.add(detalle);
        detalle.setOrdenProduccion(this);
    }
}