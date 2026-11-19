package pe.edu.upeu.gdmerp.inventario.lote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.mapper.LoteMapper;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoteServiceImpl implements LoteService {

    private final LoteMapper loteMapper;
    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;

    @Override 
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public LoteResponse registrarIngreso(LoteRequest request) {
        if (request.fechaVencimiento().isBefore(request.fechaProduccion())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la de producción");
        }

        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Producto no encontrado con id " + request.productoId()));

        if (loteRepository.existsByNumeroLoteIgnoreCaseAndProductoId(request.numeroLote().trim(), producto.getId())) {
            throw new IllegalStateException("Ya existe el lote " + request.numeroLote() + " para este producto");
        }

        BigDecimal cantidad = request.cantidadActual().setScale(4, RoundingMode.HALF_UP);

        Lote lote = loteMapper.toEntity(request);
        lote.setNumeroLote(request.numeroLote().trim().toUpperCase());
        lote.setProducto(producto);
        lote.setCantidadActual(cantidad);

        Lote guardado = loteRepository.save(lote);

        // Actualización automática del stock consolidado en Producto
        BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
        producto.setStockTotal(stockAnterior.add(cantidad).setScale(4, RoundingMode.HALF_UP));
        productoRepository.save(producto);

        return loteMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<LoteResponse> listarPorProducto(Long productoId) {
        return loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId).stream()
                .map(loteMapper::toResponse)
                .collect(Collectors.toList());
    }

}