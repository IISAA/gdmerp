package pe.edu.upeu.gdmerp.compras.compra.service;

import pe.edu.upeu.gdmerp.compras.compra.dto.*;
import pe.edu.upeu.gdmerp.compras.proveedor.dto.*;
import pe.edu.upeu.gdmerp.compras.insumo.dto.*;
import pe.edu.upeu.gdmerp.compras.compra.entity.EstadoCompra;

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
