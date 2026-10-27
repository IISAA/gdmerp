package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;

/**
 * Contrato genérico para representar el consumo de un insumo o materia prima,
 * permitiendo desacoplar la capa de inventario de las entidades de producción.
 */
public interface InsumoDetalle {
    Long getMateriaPrimaId();
    BigDecimal getCantidadRequerida();
}
