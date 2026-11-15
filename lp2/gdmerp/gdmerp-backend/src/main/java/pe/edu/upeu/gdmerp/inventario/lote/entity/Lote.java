package pe.edu.upeu.gdmerp.inventario.lote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "NUMERO_LOTE", nullable = false, length = 50)
    private String numeroLote;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "CANTIDAD_ACTUAL", nullable = false)
    private Integer cantidadActual;

    @Column(name = "FECHA_PRODUCCION", nullable = false)
    private LocalDate fechaProduccion;

    @Column(name = "FECHA_VENCIMIENTO", nullable = false)
    private LocalDate fechaVencimiento;

    @Version
    @Column(name = "VERSION")
    private Long version;
}