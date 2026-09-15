package pe.edu.upeu.gdmerp.inventario.almacen.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlmacenRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    String nombre,

    @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
    String ubicacion
) {}