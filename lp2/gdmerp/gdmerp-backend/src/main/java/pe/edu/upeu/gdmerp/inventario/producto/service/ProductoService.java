package pe.edu.upeu.gdmerp.inventario.producto.service;

import org.springframework.data.domain.Sort;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CreateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.UpdateProductoRequest;
import java.util.List;

public interface ProductoService {
    List<ProductoResponse> listar();
    List<ProductoResponse> listarPorAlmacen(Long almacenId); // <- Requerido por el endpoint del controlador
    ProductoResponse buscarPorId(Long id);
    ProductoResponse crear(CreateProductoRequest request);
    ProductoResponse actualizar(Long id, UpdateProductoRequest request);
    void eliminar(Long id);
    List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, Integer minStock, Sort sort);
}