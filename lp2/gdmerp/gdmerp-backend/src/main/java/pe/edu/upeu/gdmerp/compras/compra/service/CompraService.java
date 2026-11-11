package pe.edu.upeu.gdmerp.compras.compra.service;

import pe.edu.upeu.gdmerp.compras.compra.dto.AmortizarPagoRequest;
import pe.edu.upeu.gdmerp.compras.compra.dto.CompraReporte;
import pe.edu.upeu.gdmerp.compras.compra.dto.CompraRequest;
import pe.edu.upeu.gdmerp.compras.compra.dto.CompraResponse;
import pe.edu.upeu.gdmerp.compras.compra.dto.PagoCompraResponse;
import pe.edu.upeu.gdmerp.compras.compra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.insumo.dto.InsumoDto;
import pe.edu.upeu.gdmerp.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.gdmerp.compras.proveedor.dto.ProveedorResponse;

import java.time.LocalDate;
import java.util.List;

public interface CompraService {
    ProveedorResponse crearProveedor(ProveedorRequest request);
    List<ProveedorResponse> listarProveedores();
    CompraResponse registrarCompra(CompraRequest request);
    CompraResponse amortizarPago(Long compraId, AmortizarPagoRequest request);
    CompraResponse anularCompra(Long id);
    CompraResponse obtener(Long id);
    List<CompraResponse> listarCompras();
    List<CompraResponse> listarCompras(EstadoCompra estado, LocalDate desde, LocalDate hasta);
    CompraReporte reporte(EstadoCompra estado, LocalDate desde, LocalDate hasta);
    List<InsumoDto> listarInsumos();
    List<PagoCompraResponse> listarPagosPorCompra(Long compraId);
}
