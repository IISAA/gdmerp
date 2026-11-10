package pe.edu.upeu.gdmerp.catalogo.producto.service;

import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;

import java.util.List;

public interface ProductoService {
    default List<ProductoResponse> listar() {
        return listar(null);
    }
    List<ProductoResponse> listar(Long categoriaId);
    ProductoResponse obtener(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
    void descontarStock(Long id, Integer cantidad);
    void aumentarStock(Long id, Integer cantidad);
    void restaurarStock(Long id, Integer cantidad);
}
