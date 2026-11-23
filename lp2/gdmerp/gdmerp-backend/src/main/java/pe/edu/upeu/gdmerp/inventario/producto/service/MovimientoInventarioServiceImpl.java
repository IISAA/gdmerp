package pe.edu.upeu.gdmerp.inventario.producto.service;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.MovimientoInventarioResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.InsufficientStockException;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<MovimientoInventarioResponse> listarKardex(
            Long productoId, String tipoMovimiento, LocalDate desde, LocalDate hasta) {

        Specification<MovimientoInventario> spec = (root, query, cb) -> {
            root.fetch("producto", JoinType.INNER);
            List<Predicate> predicates = new ArrayList<>();

            if (productoId != null) {
                predicates.add(cb.equal(root.get("producto").get("id"), productoId));
            }
            if (tipoMovimiento != null && !tipoMovimiento.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("tipoMovimiento")), tipoMovimiento.trim().toUpperCase()));
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaMovimiento"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaMovimiento"), hasta.atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort orden = Sort.by(Sort.Direction.DESC, "fechaMovimiento", "id");
        return movimientoRepository.findAll(spec, orden)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public MovimientoInventarioResponse registrarMovimientoManual(MovimientoInventarioRequest request) {
        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Producto no encontrado con ID: " + request.productoId()));

        String tipo = request.tipoMovimiento().trim().toUpperCase();
        BigDecimal cantidad = request.cantidad().setScale(4, RoundingMode.HALF_UP);
        BigDecimal stockAnterior = (producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal stockResultante;

        switch (tipo) {
            case "ENTRADA", "AJUSTE_POSITIVO" -> stockResultante = stockAnterior.add(cantidad);
            case "SALIDA", "AJUSTE_NEGATIVO" -> {
                if (stockAnterior.compareTo(cantidad) < 0) {
                    throw new InsufficientStockException(String.format(
                            "Stock insuficiente para '%s'. Disponible: %s, requerido: %s",
                            producto.getNombre(),
                            stockAnterior.stripTrailingZeros().toPlainString(),
                            cantidad.stripTrailingZeros().toPlainString()));
                }
                stockResultante = stockAnterior.subtract(cantidad);
            }
            default -> throw new IllegalArgumentException(
                    "Tipo de movimiento no válido: " + tipo
                            + ". Use ENTRADA, SALIDA, AJUSTE_POSITIVO o AJUSTE_NEGATIVO.");
        }

        BigDecimal costoUnitario = request.costoUnitario() != null
                ? request.costoUnitario().setScale(4, RoundingMode.HALF_UP)
                : (producto.getCostoPromedio() != null ? producto.getCostoPromedio() : BigDecimal.ZERO);

        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                tipo,
                cantidad,
                stockAnterior,
                stockResultante,
                costoUnitario,
                request.referencia().trim());

        return toResponse(movimientoRepository.save(movimiento));
    }

    private MovimientoInventarioResponse toResponse(MovimientoInventario m) {
        Producto p = m.getProducto();
        return new MovimientoInventarioResponse(
                m.getId(),
                p.getId(),
                p.getSku(),
                p.getNombre(),
                p.getUnidadMedida(),
                m.getTipoMovimiento(),
                m.getCantidad(),
                m.getStockAnterior(),
                m.getStockResultante(),
                m.getCostoUnitario(),
                m.getFechaMovimiento(),
                m.getReferencia());
    }
}