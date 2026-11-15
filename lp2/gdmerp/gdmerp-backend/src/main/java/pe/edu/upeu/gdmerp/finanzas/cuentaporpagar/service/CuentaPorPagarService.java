package pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.service;

import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CrearCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.CuentaPorPagarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.dto.RegistrarPagoCuentaPorPagarRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporpagar.entity.EstadoCuentaPorPagar;

import java.util.List;

public interface CuentaPorPagarService {

    List<CuentaPorPagarResponse> listarTodos(EstadoCuentaPorPagar estado);

    CuentaPorPagarDetalleResponse obtenerPorId(Long id);

    CuentaPorPagarResponse registrarCuentaPorPagar(CrearCuentaPorPagarRequest req);

    CuentaPorPagarDetalleResponse registrarPago(Long id, RegistrarPagoCuentaPorPagarRequest req);
}
