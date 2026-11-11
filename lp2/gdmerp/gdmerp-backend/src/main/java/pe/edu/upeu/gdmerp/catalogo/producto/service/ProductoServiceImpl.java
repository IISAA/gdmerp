package pe.edu.upeu.gdmerp.catalogo.producto.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.catalogo.categoria.dto.CategoriaResumen;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.catalogo.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoRequest;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.ProductoResponse;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;
import pe.edu.upeu.gdmerp.catalogo.producto.repository.ProductoRepository;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;

import java.util.List;

@Service("catalogoProductoService")
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoServiceImpl(
            @Qualifier("catalogoProductoRepository") ProductoRepository productoRepository,
            @Qualifier("catalogoCategoriaRepository") CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(Long categoriaId) {
        List<Producto> productos = (categoriaId != null)
                ? productoRepository.findByCategoria_Id(categoriaId)
                : productoRepository.findAll();
        return productos.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return toResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = null;
        if (request.getCategoriaId() != null) {
            categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
        }

        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .imagenUrl(request.getImagenUrl())
                .categoria(categoria)
                .build();

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
        }

        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        if (request.getImagenUrl() != null) {
            producto.setImagenUrl(request.getImagenUrl());
        }

        return toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        if (!productoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        }
        productoRepository.deleteById(id);
    }

    private ProductoResponse toResponse(Producto p) {
        CategoriaResumen cat = null;
        if (p.getCategoria() != null) {
            cat = new CategoriaResumen(p.getCategoria().getId(), p.getCategoria().getNombre());
        }
        return ProductoResponse.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .imagenUrl(p.getImagenUrl() != null && !p.getImagenUrl().isBlank()
                        ? p.getImagenUrl()
                        : "/assets/productos/producto-" + p.getId() + ".png")
                .categoria(cat)
                .build();
    }
}
