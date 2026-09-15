package pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;

@Entity
@Table(name = "CENTROS_TRABAJO", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class CentroTrabajo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "CAPACIDAD")
    private Integer capacidad;

    @OneToMany(mappedBy = "centroTrabajo", fetch = FetchType.LAZY)
    private List<OrdenProduccion> ordenes = new ArrayList<>();
}