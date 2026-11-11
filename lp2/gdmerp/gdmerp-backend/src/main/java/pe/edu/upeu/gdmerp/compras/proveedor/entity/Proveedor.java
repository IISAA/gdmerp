package pe.edu.upeu.gdmerp.compras.proveedor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PROVEEDORES", schema = "GDM_COMPRAS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUC", nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(name = "RAZON_SOCIAL", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "EMAIL", length = 80)
    private String email;

    @Column(name = "DIRECCION", length = 200)
    private String direccion;
}
