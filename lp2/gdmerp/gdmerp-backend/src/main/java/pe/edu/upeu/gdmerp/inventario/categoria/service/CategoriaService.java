package pe.edu.upeu.gdmerp.inventario.categoria.service;

import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaRequest;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO;
import java.util.List;

public interface CategoriaService {
    List<CategoriaResponse> listar();
    CategoriaResponse buscarPorId(Long id);
    CategoriaResponse crear(CategoriaRequest request);
    CategoriaResponse actualizar(Long id, CategoriaRequest request);
    void eliminar(Long id);
    List<ReporteCategoriaCostoDTO> obtenerReporteCostosPorCategoria();
}
