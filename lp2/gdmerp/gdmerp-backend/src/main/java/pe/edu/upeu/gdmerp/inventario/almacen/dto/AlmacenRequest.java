package pe.edu.upeu.gdmerp.inventario.almacen.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AlmacenRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    String nombre,

    @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
    String ubicacion,

    @DecimalMin(value = "0.0", inclusive = true, message = "La capacidad debe ser mayor o igual a 0")
    @Digits(integer = 8, fraction = 4, message = "La capacidad no debe superar 8 dígitos enteros y 4 decimales")
    BigDecimal capacidad
) {
    public AlmacenRequest(String nombre, String ubicacion) {
        this(nombre, ubicacion, null);
    }
}
