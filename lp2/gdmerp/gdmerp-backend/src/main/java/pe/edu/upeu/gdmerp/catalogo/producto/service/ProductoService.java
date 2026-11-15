package pe.edu.upeu.gdmerp.catalogo.producto.service;

import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductoService {

    default List<ProductoResponse> listar() {
        return listar(null);
    }

    List<ProductoResponse> listar(Long categoriaId);

    List<ProductoResponse> listarPorTipo(TipoProducto tipoProducto);

    ProductoResponse obtener(Long id);

    Optional<ProductoResponse> buscarPorSku(String sku);

    Map<Long, ProductoResponse> listarPorIds(Collection<Long> ids);

    boolean existePorId(Long id);

    void validarExistencia(Long id);

    ProductoResponse crear(ProductoRequest request);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    void eliminar(Long id);

    Map<String, String> sugerirSku(String contexto, String nombreProducto);

    String generarSku(String contexto, String nombreProducto);

    String generarPrefijoSku(String contexto, String nombreProducto);
}
