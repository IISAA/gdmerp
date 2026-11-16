package pe.edu.upeu.gdmerp.compras.ordencompra.service;

import pe.edu.upeu.gdmerp.compras.ordencompra.entity.EstadoCompra;
import pe.edu.upeu.gdmerp.compras.ordencompra.entity.OrdenCompra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato del servicio de Compras bajo Bounded Context estricto.
 */
public interface ComprasService {

    OrdenCompra crearCotizacionBorrador(Long proveedorId, LocalDate fechaEsperada, String condicionPago, String notas, List<LineaOrdenDto> lineas);

    OrdenCompra confirmarOrden(Long ordenId);

    OrdenCompra anularOrden(Long ordenId, String motivo);

    OrdenCompra obtenerPorId(Long ordenId);

    List<OrdenCompra> listarPorEstado(EstadoCompra estado);

    record LineaOrdenDto(
            Long productoId,
            BigDecimal cantidadSolicitada,
            BigDecimal precioPactado
    ) {}
}
