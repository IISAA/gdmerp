package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionUpdateRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrden;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.EstadoOrdenProduccion;

import java.math.BigDecimal;
import java.util.List;

public interface OrdenProduccionService {
    List<OrdenProduccionResponse> listar();
    List<OrdenProduccionResponse> listarPorCentroTrabajo(Long centroTrabajoId);
    OrdenProduccionResponse buscarPorId(Long id);
    OrdenProduccionResponse crear(OrdenProduccionRequest request);
    OrdenProduccionResponse actualizar(Long id, OrdenProduccionUpdateRequest request);
    OrdenProduccionResponse iniciarProduccion(Long id);
    OrdenProduccionResponse procesarTransicionEstado(Long id, EstadoOrden nuevoEstado);
    OrdenProduccionResponse procesarTransicionEstado(Long id, EstadoOrdenProduccion nuevoEstado);
    BigDecimal calcularCostoTotalOrden(Long id);
    void eliminar(Long id);
}
