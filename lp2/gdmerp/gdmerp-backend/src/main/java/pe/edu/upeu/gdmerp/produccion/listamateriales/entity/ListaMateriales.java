package pe.edu.upeu.gdmerp.produccion.listamateriales.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LISTAS_MATERIALES", schema = "GDM_PRODUCCION")
@Getter
@Setter
@NoArgsConstructor
public class ListaMateriales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCTO_TERMINADO_ID", nullable = false)
    private Producto productoTerminado;

    @Column(name = "CANTIDAD_BASE", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadBase = BigDecimal.ONE;

    @OneToMany(mappedBy = "listaMateriales", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleListaMateriales> detalles = new ArrayList<>();

    public void addDetalle(DetalleListaMateriales detalle) {
        detalles.add(detalle);
        detalle.setListaMateriales(this);
    }

    public void removeDetalle(DetalleListaMateriales detalle) {
        detalles.remove(detalle);
        detalle.setListaMateriales(null);
    }
}
