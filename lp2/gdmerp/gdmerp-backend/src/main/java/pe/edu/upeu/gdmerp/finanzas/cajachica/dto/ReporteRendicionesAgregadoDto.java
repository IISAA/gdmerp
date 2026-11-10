package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para reportes y agregaciones matemáticas (SUM, AVG, MAX, MIN, COUNT).
 * Diseñado con protección contra valores nulos:
 * Si la base de datos o la consulta no tiene registros, inicializa en 0 / 0.00 en lugar de null.
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ReporteRendicionesAgregadoDto {

    private BigDecimal totalMonto;
    private Double promedioMonto;
    private BigDecimal montoMaximo;
    private BigDecimal montoMinimo;
    private Long cantidadRendiciones;

    public ReporteRendicionesAgregadoDto(BigDecimal totalMonto, Double promedioMonto, BigDecimal montoMaximo, BigDecimal montoMinimo, Long cantidadRendiciones) {
        this.totalMonto = (totalMonto != null) ? totalMonto : BigDecimal.ZERO;
        this.promedioMonto = (promedioMonto != null) ? promedioMonto : 0.0;
        this.montoMaximo = (montoMaximo != null) ? montoMaximo : BigDecimal.ZERO;
        this.montoMinimo = (montoMinimo != null) ? montoMinimo : BigDecimal.ZERO;
        this.cantidadRendiciones = (cantidadRendiciones != null) ? cantidadRendiciones : 0L;
    }

    /**
     * Factory method para crear un reporte vacío con valores inicializados en cero (0, no null).
     */
    public static ReporteRendicionesAgregadoDto empty() {
        return new ReporteRendicionesAgregadoDto(BigDecimal.ZERO, 0.0, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    }
}
