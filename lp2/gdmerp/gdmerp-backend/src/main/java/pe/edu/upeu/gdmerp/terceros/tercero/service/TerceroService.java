package pe.edu.upeu.gdmerp.terceros.tercero.service;

import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroRequest;
import pe.edu.upeu.gdmerp.terceros.tercero.dto.TerceroResponse;

import java.util.List;

public interface TerceroService {
    List<TerceroResponse> listar(String tipo);
    TerceroResponse obtener(Long id);
    TerceroResponse crear(TerceroRequest request);
    TerceroResponse actualizar(Long id, TerceroRequest request);
    void eliminar(Long id);
}
