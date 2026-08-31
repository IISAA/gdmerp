package pe.edu.upeu.gdmerp.inventario.producto.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
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
    private final AlmacenRepository almacenRepository; // Inyección para validar FK
    private final CategoriaRepository categoriaRepository; // Inyección para validar FK
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
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorAlmacen(Long almacenId) {
        getAlmacenEntity(almacenId); // Valida que el almacén exista (404 si no)
        return productoRepository.findByAlmacenId(almacenId).stream()
            .map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = productoMapper.toEntity(request);
        producto.setAlmacen(getAlmacenEntity(request.almacenId())); // Seteo de relación
        producto.setCategoria(getCategoriaEntity(request.categoriaId())); // Seteo de relación
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = getProductoEntity(id);
        productoMapper.updateEntity(producto, request);
        producto.setAlmacen(getAlmacenEntity(request.almacenId())); // Seteo de relación
        producto.setCategoria(getCategoriaEntity(request.categoriaId())); // Seteo de relación
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

    // Helper centralizado para buscar el almacén
    private Almacen getAlmacenEntity(Long almacenId) {
        return almacenRepository.findById(almacenId)
            .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + almacenId));
    }

    // Helper centralizado para buscar la categoría
    private Categoria getCategoriaEntity(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + categoriaId));
    }
}
