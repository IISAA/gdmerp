package pe.edu.upeu.gdmerp.inventario.producto.service;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listar();
    ProductoResponse buscarPorId(Long id);
    List<ProductoResponse> listarPorAlmacen(Long almacenId);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
}