package pe.edu.upeu.gdmerp.produccion.centrotrabajo.service;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoRequest;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import java.util.List;

public interface CentroTrabajoService {
    List<CentroTrabajoResponse> listar();
    CentroTrabajoResponse buscarPorId(Long id);
    CentroTrabajoResponse crear(CentroTrabajoRequest request);
    CentroTrabajoResponse actualizar(Long id, CentroTrabajoRequest request);
    void eliminar(Long id);
}