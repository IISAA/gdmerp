package pe.edu.upeu.gdmerp.inventario.producto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCTOS", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String unidadMedida;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costoPromedio;

    // Este es el campo que resuelve tu error
    // El stock es gestionado por el servidor (operaciones de lote). Nace en 0.
    @Column(nullable = false)
    private Integer stockTotal = 0;

    @Column(nullable = false)
    private Integer stockMinimo;

    // Control de concurrencia: cada UPDATE a la fila incrementa la versión y
    // valida en el WHERE que coincida con la que leyó la transacción.
    @Version
    @Column(name = "VERSION")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALMACEN_ID", nullable = false)
    private Almacen almacen;

    // Relación con los lotes para aplicar FEFO
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lote> lotes = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORIA_ID", nullable = false)
    private Categoria categoria;
}