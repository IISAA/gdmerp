package pe.edu.upeu.gdmerp.catalogo.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "Máximo 80 caracteres")
    String nombre,

    @Size(max = 200, message = "Máximo 200 caracteres")
    String descripcion
) {}
