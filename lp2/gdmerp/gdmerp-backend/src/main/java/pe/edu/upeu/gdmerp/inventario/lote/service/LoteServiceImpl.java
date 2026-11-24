package pe.edu.upeu.gdmerp.inventario.lote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.mapper.LoteMapper;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.MovimientoInventarioRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.shared.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.shared.util.CodigoGeneratorUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoteServiceImpl implements LoteService {

    private final LoteMapper loteMapper;
    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    public LoteResponse registrarIngreso(LoteRequest request) {
        if (request.fechaVencimiento().isBefore(request.fechaProduccion())) {
            throw new IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la de producción");
        }
        if (request.cantidadActual() == null || request.cantidadActual().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad del lote debe ser mayor a cero");
        }

        Producto producto = productoRepository.findById(request.productoId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Producto no encontrado con id " + request.productoId()));

        String codigoNormalizado = CodigoGeneratorUtil.normalizarCodigo(request.numeroLote());
        String numeroLote = (codigoNormalizado != null) ? codigoNormalizado : generarSiguienteNumeroLote();

        if (loteRepository.existsByNumeroLoteIgnoreCaseAndProductoId(numeroLote, producto.getId())) {
            throw new IllegalStateException("Ya existe el lote " + numeroLote + " para este producto");
        }

        BigDecimal cantidad = request.cantidadActual().setScale(4, RoundingMode.HALF_UP);

        Lote lote = loteMapper.toEntity(request);
        lote.setNumeroLote(numeroLote);
        lote.setProducto(producto);
        lote.setCantidadActual(cantidad);

        Lote guardado = loteRepository.save(lote);

        // Actualización automática del stock consolidado en Producto
        BigDecimal stockAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : BigDecimal.ZERO;
        BigDecimal stockResultante = stockAnterior.add(cantidad).setScale(4, RoundingMode.HALF_UP);
        producto.setStockTotal(stockResultante);
        productoRepository.save(producto);

        // Registro automático en el Kardex
        BigDecimal costo = producto.getCostoPromedio() != null ? producto.getCostoPromedio() : BigDecimal.ZERO;
        MovimientoInventario movimiento = new MovimientoInventario(
                producto,
                "ENTRADA_LOTE",
                cantidad,
                stockAnterior.setScale(4, RoundingMode.HALF_UP),
                stockResultante,
                costo,
                "Ingreso de lote " + numeroLote);
        movimientoRepository.save(movimiento);

        return loteMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<LoteResponse> listarPorProducto(Long productoId) {
        return loteRepository.findByProductoIdOrderByFechaVencimientoAsc(productoId).stream()
                .map(loteMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<LoteResponse> listarTodos() {
        return loteRepository.findAll(Sort.by("fechaVencimiento").ascending())
                .stream()
                .map(loteMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public String obtenerSiguienteCodigo() {
        return generarSiguienteNumeroLote();
    }

    private String generarSiguienteNumeroLote() {
        String prefijo = "LT-" + LocalDate.now().getYear();
        long conteo = loteRepository.countByNumeroLoteStartingWith(prefijo);
        return CodigoGeneratorUtil.generarCorrelativoUnico(
                prefijo, conteo, 3, loteRepository::existsByNumeroLoteIgnoreCase
        );
    }
}