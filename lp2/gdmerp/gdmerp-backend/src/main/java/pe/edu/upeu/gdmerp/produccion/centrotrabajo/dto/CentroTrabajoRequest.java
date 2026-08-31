package pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record CentroTrabajoRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    @Min(value = 0, message = "La capacidad no puede ser menor a 0") Integer capacidad
) {}