package pe.edu.upeu.gdmerp.inventario.lote.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import java.time.LocalDate;

@Entity
@Table(name = "LOTES", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
public class Lote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String numeroLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCTO_ID", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidadActual;

    @Column(nullable = false)
    private LocalDate fechaProduccion;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    // Control de concurrencia sobre la cantidad disponible del lote (consumo FEFO).
    @Version
    @Column(name = "VERSION")
    private Long version;
}