package pe.edu.upeu.gdmerp.finanzas.sesioncaja.service;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse;

import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.AperturaSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreSesionRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaRequest;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaDetalleResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;

import java.util.List;

public interface SesionCajaService {

    SesionCajaResponse abrirSesion(AperturaSesionRequest request);

    SesionCajaResponse cerrarSesion(Long id, CierreSesionRequest request);

    MovimientoCajaResponse registrarMovimiento(Long sesionId, MovimientoCajaRequest request);

    List<SesionCajaResponse> listarSesiones();

    SesionCajaDetalleResponse obtenerDetalle(Long id);

    SesionCajaResponse obtenerSesionActivaUsuario(Long usuarioId);

    java.util.Optional<SesionCajaResponse> buscarUltimaSesionActiva();

    SesionCajaResponse obtenerUltimaSesionActiva();

    CierreCajaReporteDto generarDatosReporte(Long id);

    List<pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.DenominacionMonedaResponse> listarDenominaciones();
}
