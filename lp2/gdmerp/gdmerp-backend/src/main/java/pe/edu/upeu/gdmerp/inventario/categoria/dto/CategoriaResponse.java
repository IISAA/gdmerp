package pe.edu.upeu.gdmerp.inventario.categoria.dto;

import java.io.Serializable;

public record CategoriaResponse(Long id, String nombre, String descripcion) implements Serializable {
}
