package pe.edu.upeu.gdmerp.inventario.categoria.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaRequest;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.CategoriaResponse;
import pe.edu.upeu.gdmerp.inventario.categoria.dto.ReporteCategoriaCostoDTO;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(categoriaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaMapper.toResponse(getCategoriaEntity(id));
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        validarNombreUnico(request.nombre(), null);
        return categoriaMapper.toResponse(categoriaRepository.save(categoriaMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = getCategoriaEntity(id);
        validarNombreUnico(request.nombre(), id);
        categoriaMapper.updateEntity(categoria, request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = getCategoriaEntity(id);
        if (productoRepository.existsByCategoriaId(id)) {
            throw new IllegalStateException("No se puede eliminar la categoría con id " + id + " porque tiene productos asociados");
        }
        categoriaRepository.delete(categoria);
    }

    private void validarNombreUnico(String nombre, Long idActual) {
        boolean existe = (idActual == null)
                ? categoriaRepository.existsByNombre(nombre)
                : categoriaRepository.existsByNombreAndIdNot(nombre, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe una categoría con el nombre: " + nombre);
        }
    }

    private Categoria getCategoriaEntity(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteCategoriaCostoDTO> obtenerReporteCostosPorCategoria() {
        return categoriaRepository.reporteCostosPorCategoria();
    }
}