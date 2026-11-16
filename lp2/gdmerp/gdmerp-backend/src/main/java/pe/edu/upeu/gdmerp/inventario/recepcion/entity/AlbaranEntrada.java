package pe.edu.upeu.gdmerp.inventario.recepcion.entity;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregado del Bounded Context INVENTARIO que gestiona la recepción física de mercancía.
 * Se inicializa en estado PENDIENTE al confirmarse una Orden de Compra.
 */
@Entity
@Table(name = "ALBARANES_ENTRADA", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbaranEntrada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_albaranes_entrada")
    @SequenceGenerator(
            name = "seq_albaranes_entrada",
            sequenceName = "SEQ_ALBARANES_ENTRADA",
            schema = "GDM_INVENTARIO",
            allocationSize = 50
    )
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(name = "ORDEN_COMPRA_ID", nullable = false)
    private Long ordenCompraId;

    @Column(name = "PROVEEDOR_ID", nullable = false)
    private Long proveedorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoAlbaran estado;

    @Column(name = "FECHA_CREACION", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "FECHA_RECEPCION")
    private LocalDateTime fechaRecepcion;

    @Builder.Default
    @OneToMany(mappedBy = "albaranEntrada", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAlbaranEntrada> detalles = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoAlbaran.PENDIENTE;
        }
    }
}
