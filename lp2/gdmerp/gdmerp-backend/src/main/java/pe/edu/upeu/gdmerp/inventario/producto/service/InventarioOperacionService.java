package pe.edu.upeu.gdmerp.inventario.producto.service;

import java.time.LocalDate;

public interface InventarioOperacionService {
    void consumirStockFefo(Long productoId, Integer cantidadRequerida);
    String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento);
}