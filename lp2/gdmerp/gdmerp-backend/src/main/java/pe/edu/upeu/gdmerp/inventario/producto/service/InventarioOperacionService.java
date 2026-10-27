package pe.edu.upeu.gdmerp.inventario.producto.service;

import pe.edu.upeu.gdmerp.inventario.producto.dto.InsumoDetalle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventarioOperacionService {
    void consumirStockFefo(Long productoId, BigDecimal cantidadRequerida);
    void consumirStockFefo(Long productoId, Integer cantidadRequerida);
    String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento);
    String registrarIngresoLote(Long productoId, BigDecimal cantidad, LocalDate fechaVencimiento);
    void registrarIngresoProduccion(Long productoId, Integer cantidad, String lote);
    void registrarIngresoProduccion(Long productoId, BigDecimal cantidad, String lote);
    void registrarIngresoProduccion(String nombreProducto, Integer cantidad, String lote);
    void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad, String lote);
    void registrarIngresoProduccion(String nombreProducto, BigDecimal cantidad);

    void descontarInsumosProduccion(List<? extends InsumoDetalle> detalles);
    void reversarInsumosProduccion(List<? extends InsumoDetalle> detalles);
}
