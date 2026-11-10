package pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.service;

import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CrearCreditoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.CuentaPorCobrarResponse;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.dto.RegistrarAbonoRequest;
import pe.edu.upeu.gdmerp.finanzas.cuentaporcobrar.entity.EstadoCredito;

import java.util.List;

public interface CuentaPorCobrarService {
    List<CuentaPorCobrarResponse> listarTodos(EstadoCredito estado);
    CuentaPorCobrarDetalleResponse obtenerPorId(Long id);
    CuentaPorCobrarResponse registrarCredito(CrearCreditoRequest req);
    CuentaPorCobrarDetalleResponse registrarAbono(Long creditoId, RegistrarAbonoRequest req);
}
