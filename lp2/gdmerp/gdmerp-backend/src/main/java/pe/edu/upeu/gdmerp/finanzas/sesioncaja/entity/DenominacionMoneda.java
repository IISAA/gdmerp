package pe.edu.upeu.gdmerp.finanzas.sesioncaja.entity;

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
@Table(name = "DENOMINACIONES_MONEDA")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DenominacionMoneda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "VALOR_FACIAL", nullable = false, precision = 8, scale = 2)
    private BigDecimal valorFacial;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 15)
    private TipoDenominacion tipo;

    @Column(name = "ORDEN", nullable = false)
    private Integer orden;

    @Column(name = "ACTIVO", nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
