package pe.edu.upeu.gdmerp.inventario.categoria.entity;

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
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

@Entity
@Table(name = "CATEGORIAS", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 200)
    private String descripcion;

    // Lado "1" de la relación: una categoría clasifica muchos productos.
    // Sin cascade: la FK la posee Producto, el borrado en cascada de productos
    // no debe originarse desde aquí.
    @OneToMany(mappedBy = "categoria", fetch = FetchType.LAZY)
    private List<Producto> productos = new ArrayList<>();
}
