package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.service.ProductoService;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.DetalleConsumoDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ReporteConsumoOrdenDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.ResumenEstadoOrdenesDTO;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.DetalleOrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenProduccionReporteServiceImpl implements OrdenProduccionReporteService {

    private final OrdenProduccionRepository ordenRepository;
    private final ProductoService productoService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public List<ResumenEstadoOrdenesDTO> obtenerResumenEstados() {
        return ordenRepository.reporteEstadoOrdenes();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'JEFE_PRODUCCION', 'OPERARIO', 'ALMACENERO')")
    public ReporteConsumoOrdenDTO calcularConsumo(Long ordenId) {
        OrdenProduccion orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + ordenId));

        List<DetalleOrdenProduccion> detalles = orden.getDetalles();
        List<Long> productoIds = (detalles != null)
                ? detalles.stream()
                        .filter(detalle -> detalle != null && detalle.getMateriaPrimaId() != null)
                        .map(detalle -> detalle.getMateriaPrimaId())
                        .toList()
                : List.of();

        Map<Long, BigDecimal> costos = productoService.obtenerCostoPromedioPorIds(productoIds)
                .stream()
                .filter(dto -> dto != null && dto.productoId() != null)
                .collect(Collectors.toMap(
                        dto -> dto.productoId(),
                        dto -> dto.costoPromedio() != null ? dto.costoPromedio() : BigDecimal.ZERO,
                        (existente, reemplazo) -> existente
                ));

        BigDecimal costoTotal = BigDecimal.ZERO;
        List<DetalleConsumoDTO> materiales = new ArrayList<>();
        if (detalles != null) {
            for (DetalleOrdenProduccion detalle : detalles) {
                if (detalle == null) {
                    continue;
                }
                BigDecimal costoUnitario = costos.get(detalle.getMateriaPrimaId());
                if (costoUnitario == null) {
                    costoUnitario = detalle.getCostoUnitario() != null ? detalle.getCostoUnitario() : BigDecimal.ZERO;
                }
                BigDecimal cantidad = detalle.getCantidadRequerida() != null ? detalle.getCantidadRequerida() : BigDecimal.ZERO;
                BigDecimal costoLinea = costoUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
                costoTotal = costoTotal.add(costoLinea);
                materiales.add(new DetalleConsumoDTO(
                        detalle.getMateriaPrimaId(),
                        detalle.getNombreProducto(),
                        cantidad,
                        costoUnitario,
                        costoLinea));
            }
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
