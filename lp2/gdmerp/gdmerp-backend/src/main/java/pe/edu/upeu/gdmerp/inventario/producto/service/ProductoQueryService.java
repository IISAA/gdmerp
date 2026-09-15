package pe.edu.upeu.gdmerp.inventario.producto.service;

import pe.edu.upeu.gdmerp.inventario.producto.dto.CostoProductoDTO;
import java.util.List;

public interface ProductoQueryService {
    List<CostoProductoDTO> obtenerCostoPromedioPorIds(List<Long> productoIds);
}