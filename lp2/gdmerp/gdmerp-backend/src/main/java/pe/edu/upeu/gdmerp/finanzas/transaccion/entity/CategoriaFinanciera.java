package pe.edu.upeu.gdmerp.finanzas.transaccion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CATEGORIAS_FINANCIERAS", schema = "GDM_FINANZAS")
@Getter
@Setter
@NoArgsConstructor
public class CategoriaFinanciera {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_categorias_financieras")
    @SequenceGenerator(name = "seq_categorias_financieras", sequenceName = "SEQ_CATEGORIAS_FINANCIERAS", schema = "GDM_FINANZAS", allocationSize = 50)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NOMBRE", nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(name = "DESCRIPCION", length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_DEFAULT", nullable = false, length = 20)
    private TipoFinanciero tipoDefault;
}
