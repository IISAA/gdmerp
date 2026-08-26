package pe.edu.upeu.gdmerp.inventario.producto.dto;

import java.math.BigDecimal;

public record ProductoResponse(Long id, String nombre, BigDecimal precio, Integer stock) {
}