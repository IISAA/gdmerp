package pe.edu.upeu.gdmerp.inventario.stock.dto;

import java.math.BigDecimal;

/**
 * Contrato genérico para representar el consumo de un insumo o materia prima,
 * desacoplando la capa de inventario de las entidades de producción.
 */
public interface InsumoDetalle {
    Long getMateriaPrimaId();

    BigDecimal getCantidadRequerida();
}
