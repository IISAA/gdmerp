package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CreateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.inventario.producto.dto.UpdateProductoRequest;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;
import pe.edu.upeu.gdmerp.inventario.producto.mapper.ProductoMapper;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final CategoriaRepository categoriaRepository;
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
    public ProductoResponse crear(CreateProductoRequest request) {
        validarSkuUnico(request.sku(), null);

        Producto producto = productoMapper.toEntity(request);
        producto.setAlmacen(getAlmacenEntity(request.almacenId()));
        producto.setCategoria(getCategoriaEntity(request.categoriaId()));
        producto.setStockTotal(0);

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, UpdateProductoRequest request) {
        Producto producto = getProductoEntity(id);
        validarSkuUnico(request.sku(), id);

        productoMapper.updateEntity(producto, request);
        producto.setAlmacen(getAlmacenEntity(request.almacenId()));
        producto.setCategoria(getCategoriaEntity(request.categoriaId()));

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = getProductoEntity(id);
        if (producto.getStockTotal() > 0) {
            throw new IllegalStateException("No se puede eliminar el producto con id " + id + " porque aún tiene stock");
        }
        productoRepository.delete(producto);
    }

    private void validarSkuUnico(String sku, Long idActual) {
        boolean existe = (idActual == null)
                ? productoRepository.existsBySku(sku)
                : productoRepository.existsBySkuAndIdNot(sku, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe un producto con el SKU: " + sku);
        }
    }

    private Producto getProductoEntity(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id " + id));
    }

    private Almacen getAlmacenEntity(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + id));
    }

    private Categoria getCategoriaEntity(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorAlmacen(Long almacenId) {
        return productoRepository.findByAlmacenId(almacenId)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> filtrarDinamicamente(Long categoriaId, Long almacenId, Integer minStock, Sort sort) {
        return productoRepository.filtrarDinamicamente(categoriaId, almacenId, minStock, sort)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }
}