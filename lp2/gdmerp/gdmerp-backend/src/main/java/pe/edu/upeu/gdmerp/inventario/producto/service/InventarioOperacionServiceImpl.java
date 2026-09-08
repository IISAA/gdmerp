package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;
import pe.edu.upeu.gdmerp.inventario.lote.repository.LoteRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventarioOperacionServiceImpl implements InventarioOperacionService {
    
    private final ProductoRepository productoRepository;
    private final LoteRepository loteRepository;

    @Override
    @Transactional
    public void consumirStockFefo(Long productoId, Integer cantidadRequerida) {
        Producto producto = getProducto(productoId);
        
        if (producto.getStockTotal() < cantidadRequerida) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + producto.getNombre());
        }

        List<Lote> lotesDisponibles = loteRepository.findByProductoIdAndCantidadActualGreaterThanOrderByFechaVencimientoAsc(productoId, 0);

        int cantidadPendiente = cantidadRequerida;

        for (Lote lote : lotesDisponibles) {
            if (cantidadPendiente <= 0) break;

            int cantidadAExtraer = Math.min(lote.getCantidadActual(), cantidadPendiente);
            lote.setCantidadActual(lote.getCantidadActual() - cantidadAExtraer);
            loteRepository.save(lote);
            
            cantidadPendiente -= cantidadAExtraer;
        }

        producto.setStockTotal(producto.getStockTotal() - cantidadRequerida);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public String registrarIngresoLote(Long productoId, Integer cantidad, LocalDate fechaVencimiento) {
        Producto producto = getProducto(productoId);
        
        String numeroLote = "LT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Lote nuevoLote = new Lote();
        nuevoLote.setProducto(producto);
        nuevoLote.setNumeroLote(numeroLote);
        nuevoLote.setCantidadActual(cantidad);
        nuevoLote.setFechaProduccion(LocalDate.now());
        nuevoLote.setFechaVencimiento(fechaVencimiento);
        loteRepository.save(nuevoLote);

        producto.setStockTotal(producto.getStockTotal() + cantidad);
        productoRepository.save(producto);

        return numeroLote;
    }

    private Producto getProducto(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }
}