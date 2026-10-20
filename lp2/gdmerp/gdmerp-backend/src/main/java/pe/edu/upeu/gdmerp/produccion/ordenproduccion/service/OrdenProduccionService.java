package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import java.util.List;

public interface OrdenProduccionService {
    List<OrdenProduccionResponse> listar();
    List<OrdenProduccionResponse> listarPorCentroTrabajo(Long centroTrabajoId); // <- Agregar esta línea
    OrdenProduccionResponse buscarPorId(Long id);
    OrdenProduccionResponse crear(OrdenProduccionRequest request);
    OrdenProduccionResponse actualizar(Long id, OrdenProduccionUpdateRequest request);
    void eliminar(Long id);
}