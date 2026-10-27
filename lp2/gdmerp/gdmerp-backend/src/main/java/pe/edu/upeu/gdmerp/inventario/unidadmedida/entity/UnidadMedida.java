package pe.edu.upeu.gdmerp.inventario.unidadmedida.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "UNIDADES_MEDIDA", schema = "GDM_INVENTARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "CODIGO_SUNAT", length = 10)
    private String codigoSunat;

    @Column(name = "NOMBRE", nullable = false, length = 80)
    private String nombre;

    @Column(name = "SIMBOLO", nullable = false, length = 15)
    private String simbolo;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORIA", nullable = false, length = 30)
    private CategoriaUnidadMedida categoria;

    @Column(name = "FACTOR_CONVERSION", precision = 12, scale = 4, nullable = false)
    private BigDecimal factorConversion;

    @Column(name = "PERMITE_DECIMALES", nullable = false)
    @Builder.Default
    private Boolean permiteDecimales = true;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
