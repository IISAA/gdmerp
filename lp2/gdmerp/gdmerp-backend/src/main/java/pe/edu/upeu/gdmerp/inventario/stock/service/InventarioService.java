package pe.edu.upeu.gdmerp.inventario.stock.service;

import org.springframework.data.domain.Sort;
import pe.edu.upeu.gdmerp.inventario.stock.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.stock.dto.StockInventarioResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface InventarioService {

    default List<StockInventarioResponse> listar() {
        return listar(null);
    }

    List<StockInventarioResponse> listar(String tipoProducto);

    StockInventarioResponse buscarPorId(Long stockId);

    StockInventarioResponse buscarPorProductoId(Long productoId);

    StockInventarioResponse registrarStock(StockInventarioRequest request);

    StockInventarioResponse actualizarStock(Long stockId, StockInventarioRequest request);

    void eliminarStock(Long stockId);

    List<StockInventarioResponse> listarPorAlmacen(Long almacenId);

    List<StockInventarioResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, BigDecimal minStock, Sort sort);

    default List<StockInventarioResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, Integer minStock, Sort sort) {
        return filtrarDinamicamente(categoriaId, almacenId, minStock != null ? BigDecimal.valueOf(minStock) : null, sort);
    }

    List<MovimientoInventarioResponse> listarMovimientosPorProducto(Long productoId);

    List<CostoProductoDTO> obtenerCostoPromedioPorIds(Collection<Long> productoIds);

    List<ReporteStockProductoDTO> obtenerReporteStock();

    List<ReporteStockCriticoDTO> obtenerReporteStockCritico();

    pe.edu.upeu.gdmerp.inventario.stock.dto.ReporteSemaforoDTO obtenerReporteSemaforo();

    void ajustarStock(Long productoId, BigDecimal cantidadDelta);

    void ajustarStock(Long productoId, Integer cantidadDelta);

    void ajustarStock(Long productoId, Double cantidadDelta);

    void registrarIngreso(Long productoId, BigDecimal cantidadIngreso);

    void registrarSalida(Long productoId, BigDecimal cantidadSalida);

    void validarStockDisponible(Long productoId, BigDecimal cantidadRequerida);

    boolean verificarAlertaStockMinimo(Long productoId);
}
