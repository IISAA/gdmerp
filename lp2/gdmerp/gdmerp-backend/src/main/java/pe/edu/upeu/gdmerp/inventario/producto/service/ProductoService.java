package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.springframework.data.domain.Sort;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ProductoService {
    List<ProductoResponse> listar(String tipoProducto);

    List<ProductoResponse> listar();

    ProductoResponse buscarPorId(Long id);

    ProductoResponse crear(ProductoRequest request);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    void eliminar(Long id);

    List<ProductoResponse> listarPorAlmacen(Long almacenId);

    List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, BigDecimal minStock, Sort sort);

    List<ReporteStockProductoDTO> reporteStockTotalPorProducto();

    List<CostoProductoDTO> obtenerCostoPromedioPorIds(List<Long> ids);

    List<ReporteStockCriticoDTO> reporteStockCritico();

    void validarStockDisponible(Long id, BigDecimal cantidadRequerida);

    boolean verificarAlertaStockMinimo(Long id);

    Map<String, String> sugerirSku(Long almacenId, String nombreProducto);
}