package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
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
    private final AlmacenRepository almacenRepository;
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
        // Se corrige la referencia a la FK, usando almacenId() del DTO
        Almacen almacen = getAlmacenEntity(request.almacenId());

        Producto producto = productoMapper.toEntity(request);
        producto.setAlmacen(almacen); // Se asigna el objeto Almacen validado

        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = getProductoEntity(id);
        productoMapper.updateEntity(producto, request);

        Almacen almacen = getAlmacenEntity(request.almacenId());
        producto.setAlmacen(almacen);

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

    private Almacen getAlmacenEntity(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorAlmacen(Long almacenId) {
        // Puedes delegar la búsqueda al repositorio filtrando por el ID del almacén
        return productoRepository.findByAlmacenId(almacenId)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }
}