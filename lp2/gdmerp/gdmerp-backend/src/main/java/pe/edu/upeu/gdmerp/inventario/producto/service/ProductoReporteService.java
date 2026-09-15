package pe.edu.upeu.gdmerp.inventario.producto.service;

import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import java.util.List;

public interface ProductoReporteService {
    List<ReporteStockProductoDTO> obtenerReporteStock();
    List<ReporteStockCriticoDTO> obtenerReporteStockCritico();
}