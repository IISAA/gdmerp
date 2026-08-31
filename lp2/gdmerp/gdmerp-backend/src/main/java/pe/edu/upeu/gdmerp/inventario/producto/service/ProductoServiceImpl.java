package pe.edu.upeu.gdmerp.inventario.producto.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.mapper.ProductoMapper;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(Long id) {
        return productoMapper.toResponse(getProductoEntity(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = productoMapper.toEntity(request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = getProductoEntity(id);
        productoMapper.updateEntity(producto, request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        productoRepository.delete(getProductoEntity(id));
    }

    private Producto getProductoEntity(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }
}