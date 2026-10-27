package pe.edu.upeu.gdmerp.produccion.listamateriales.service;

import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesRequest;
import pe.edu.upeu.gdmerp.produccion.listamateriales.dto.ListaMaterialesResponse;

import java.util.List;

public interface ListaMaterialesService {
    List<ListaMaterialesResponse> listar();
    ListaMaterialesResponse buscarPorId(Long id);
    ListaMaterialesResponse buscarPorProductoTerminadoId(Long productoTerminadoId);
    List<ListaMaterialesResponse> listarPorProductoTerminadoId(Long productoTerminadoId);
    ListaMaterialesResponse crear(ListaMaterialesRequest request);
    ListaMaterialesResponse actualizar(Long id, ListaMaterialesRequest request);
    void eliminar(Long id);
}
