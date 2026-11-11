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
import pe.edu.upeu.gdmerp.shared.event.CategoriaEliminacionSolicitadaEvent;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    @Cacheable(value = "categorias", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAll().stream().map(categoriaMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    @Cacheable(value = "categorias", key = "#id", unless = "#result == null")
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaMapper.toResponse(getCategoriaEntity(id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "categorias", allEntries = true)
    public CategoriaResponse crear(CategoriaRequest request) {
        validarNombreUnico(request.nombre(), null);
        return categoriaMapper.toResponse(categoriaRepository.save(categoriaMapper.toEntity(request)));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "categorias", allEntries = true)
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = getCategoriaEntity(id);
        validarNombreUnico(request.nombre(), id);
        categoriaMapper.updateEntity(categoria, request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "categorias", allEntries = true)
    public void eliminar(Long id) {
        Categoria categoria = getCategoriaEntity(id);
        eventPublisher.publishEvent(new CategoriaEliminacionSolicitadaEvent(categoria.getId(), categoria.getNombre()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    public List<ReporteCategoriaCostoDTO> obtenerReporteCostosPorCategoria() {
        return categoriaRepository.reporteCostosPorCategoria();
    }
}