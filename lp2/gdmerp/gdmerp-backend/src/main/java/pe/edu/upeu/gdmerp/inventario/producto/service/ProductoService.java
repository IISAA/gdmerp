package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.springframework.data.domain.Sort;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listar(String tipoProducto);
    default List<ProductoResponse> listar() {
        return listar((String) null);
    }
    List<ProductoResponse> listarPorAlmacen(Long almacenId); // <- Requerido por el endpoint del controlador
    ProductoResponse buscarPorId(Long id); // método
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
    List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, Integer minStock, Sort sort);
    void ajustarStock(Long id, Integer cantidadDelta);
    void ajustarStock(Long id, Double cantidadDelta);
}