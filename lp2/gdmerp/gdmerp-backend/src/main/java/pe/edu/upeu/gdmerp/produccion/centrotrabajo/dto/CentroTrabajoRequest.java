package pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CentroTrabajoRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    String nombre,

    @DecimalMin(value = "0.0000", message = "La capacidad no puede ser menor a 0")
    @Digits(integer = 8, fraction = 4, message = "La capacidad no debe superar 8 dígitos enteros y 4 decimales")
    BigDecimal capacidad,

    @NotBlank(message = "El código del centro de trabajo es obligatorio")
    @Size(max = 20, message = "El código no puede exceder los 20 caracteres")
    String codigo,

    @NotNull(message = "El costo por hora es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo por hora no puede ser negativo")
    BigDecimal costoHora
    
) {}