package pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;

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

    @Column(name = "PRODUCTO", nullable = false, length = 120)
    private String producto;

    @Column(name = "CANTIDAD_PLANIFICADA", nullable = false)
    private Integer cantidadPlanificada;

    @Column(name = "ESTADO", nullable = false, length = 30)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CENTRO_TRABAJO_ID", nullable = false)
    private CentroTrabajo centroTrabajo;
}