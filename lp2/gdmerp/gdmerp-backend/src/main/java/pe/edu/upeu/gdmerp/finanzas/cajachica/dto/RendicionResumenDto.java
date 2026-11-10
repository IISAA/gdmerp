package pe.edu.upeu.gdmerp.finanzas.cajachica.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.cajachica.entity.EstadoRendicion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de proyección optimizado para listar resúmenes de rendiciones.
 * Utilizado con expresiones de constructor en consultas JPQL (SELECT new ...).
 * Evita sobrecarga de memoria al no cargar la entidad completa ni colecciones pesadas.
 */
@Getter
@Setter
@Builder
public class RendicionResumenDto {

    private Long id;
    private String codigo;
    private String responsable;
    private LocalDate fecha;
    private BigDecimal montoTotal;
    private EstadoRendicion estado;
    private String codigoCajaChica;
    private Integer cantidadDetalles;

    public RendicionResumenDto() {
    }

    public RendicionResumenDto(Long id, String codigo, String responsable, LocalDate fecha, BigDecimal montoTotal, EstadoRendicion estado, String codigoCajaChica, Integer cantidadDetalles) {
        this.id = id;
        this.codigo = codigo;
        this.responsable = responsable;
        this.fecha = fecha;
        this.montoTotal = montoTotal;
        this.estado = estado;
        this.codigoCajaChica = codigoCajaChica;
        this.cantidadDetalles = cantidadDetalles;
    }
}
