package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import java.util.List;

public interface OrdenProduccionService {
    List<OrdenProduccionResponse> listar();
    OrdenProduccionResponse buscarPorId(Long id);
    OrdenProduccionResponse crear(OrdenProduccionRequest request);
    OrdenProduccionResponse actualizar(Long id, OrdenProduccionRequest request);
    void eliminar(Long id);
}