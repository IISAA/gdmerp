package pe.edu.upeu.gdmerp.finanzas.transaccion.service;

import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.reporte.dto.FlujoCajaResumenResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraRequest;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.util.List;

public interface FinanzasService {
    // Categorías Financieras (Entidad de Clasificación)
    List<CategoriaFinancieraResponse> listarCategorias(TipoFinanciero tipo);
    CategoriaFinancieraResponse obtenerCategoria(Long id);
    CategoriaFinancieraResponse crearCategoria(CategoriaFinancieraRequest request);
    CategoriaFinancieraResponse actualizarCategoria(Long id, CategoriaFinancieraRequest request);
    void eliminarCategoria(Long id);

    // Transacciones Financieras (Entidad Principal con Navegación Controlada)
    List<TransaccionFinancieraResponse> listarTransacciones(TipoFinanciero tipo, EstadoFinanciero estado, Long categoriaFinancieraId);
    List<TransaccionFinancieraResponse> listarTransaccionesPorCategoria(Long categoriaFinancieraId);
    TransaccionFinancieraResponse obtenerTransaccion(Long id);
    TransaccionFinancieraResponse crearTransaccion(TransaccionFinancieraRequest request);
    TransaccionFinancieraResponse actualizarTransaccion(Long id, TransaccionFinancieraRequest request);
    TransaccionFinancieraResponse cambiarEstadoTransaccion(Long id, EstadoFinanciero nuevoEstado);
    void eliminarTransaccion(Long id);
    FlujoCajaResumenResponse obtenerResumenFlujoCaja();
}
