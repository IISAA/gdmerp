package pe.edu.upeu.gdmerp.inventario.almacen.dto;
import jakarta.validation.constraints.NotBlank;

public record AlmacenRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    String ubicacion
) {}