package pe.edu.upeu.gdmerp.inventario.almacen.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record AlmacenResponse(
    Long id,
    String nombre,
    String ubicacion,
    BigDecimal capacidad
) implements Serializable {
    public AlmacenResponse(Long id, String nombre, String ubicacion) {
        this(id, nombre, ubicacion, null);
    }
}
