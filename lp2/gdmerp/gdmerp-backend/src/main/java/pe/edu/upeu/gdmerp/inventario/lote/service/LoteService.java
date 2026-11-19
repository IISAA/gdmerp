package pe.edu.upeu.gdmerp.inventario.lote.service;

import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;

import java.util.List;

public interface LoteService {
    LoteResponse registrarIngreso(LoteRequest request);
    List<LoteResponse> listarPorProducto(Long productoId);
}