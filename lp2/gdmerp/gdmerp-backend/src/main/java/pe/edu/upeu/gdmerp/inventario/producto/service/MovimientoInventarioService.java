package pe.edu.upeu.gdmerp.inventario.producto.service;

import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioResponse;

import java.time.LocalDate;
import java.util.List;

public interface MovimientoInventarioService {
    List<MovimientoInventarioResponse> listarKardex(Long productoId, String tipoMovimiento, LocalDate desde,
            LocalDate hasta);

    MovimientoInventarioResponse registrarMovimientoManual(MovimientoInventarioRequest request);
}