package pe.edu.upeu.gdmerp.inventario.categoria.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaRequest(
    @NotBlank(message = "El nombre es obligatorio") String nombre,
    String descripcion
) {}
