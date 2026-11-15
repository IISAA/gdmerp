package pe.edu.upeu.gdmerp.produccion.listamateriales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "DETALLES_LISTA_MATERIALES", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class DetalleListaMateriales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LISTA_MATERIALES_ID", nullable = false)
    private ListaMateriales listaMateriales;

    @Column(name = "INSUMO_ID", nullable = false)
    private Long insumoId;

    @Column(name = "CANTIDAD_REQUERIDA", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadRequerida;
}
