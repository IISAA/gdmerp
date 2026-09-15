package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoQueryService;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleConsumoDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ReporteConsumoOrdenDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenProduccionReporteServiceImpl implements OrdenProduccionReporteService {

    private final OrdenProduccionRepository ordenRepository;
    private final ProductoQueryService productoService;

    @Override
    @Transactional(readOnly = true)
    public List<ResumenEstadoOrdenesDTO> obtenerResumenEstados() {
        return ordenRepository.reporteEstadoOrdenes();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteConsumoOrdenDTO calcularConsumo(Long ordenId) {
        OrdenProduccion orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + ordenId));

        List<DetalleOrdenProduccion> detalles = orden.getDetalles();
        List<Long> productoIds = detalles.stream().map(DetalleOrdenProduccion::getProductoId).toList();

        Map<Long, BigDecimal> costos = productoService.obtenerCostoPromedioPorIds(productoIds)
                .stream()
                .collect(Collectors.toMap(CostoProductoDTO::productoId, CostoProductoDTO::costoPromedio));

        BigDecimal costoTotal = BigDecimal.ZERO;
        List<DetalleConsumoDTO> materiales = new ArrayList<>();
        for (DetalleOrdenProduccion detalle : detalles) {
            BigDecimal costoUnitario = costos.get(detalle.getProductoId());
            if (costoUnitario == null) {
                costoUnitario = detalle.getCostoUnitario() != null ? detalle.getCostoUnitario() : BigDecimal.ZERO;
            }
            BigDecimal costoLinea = costoUnitario.multiply(BigDecimal.valueOf(detalle.getCantidadRequerida()));
            costoTotal = costoTotal.add(costoLinea);
            materiales.add(new DetalleConsumoDTO(
                    detalle.getProductoId(),
                    detalle.getNombreProducto(),
                    detalle.getCantidadRequerida(),
                    costoUnitario,
                    costoLinea));
        }

        return new ReporteConsumoOrdenDTO(
                orden.getId(),
                orden.getProducto(),
                orden.getCantidadPlanificada(),
                orden.getEstado(),
                costoTotal,
                materiales);
    }
}