package pe.edu.upeu.gdmerp.inventario.almacen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.mapper.AlmacenMapper;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import pe.edu.upeu.gdmerp.shared.event.AlmacenEliminacionSolicitadaEvent;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlmacenServiceImpl implements AlmacenService {
    private final AlmacenRepository almacenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AlmacenMapper almacenMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    @Cacheable(value = "almacenes", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<AlmacenResponse> listar() {
        return almacenRepository.findAll().stream()
                .map(almacenMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO', 'JEFE_PRODUCCION', 'OPERARIO')")
    @Cacheable(value = "almacenes", key = "#id", unless = "#result == null")
    public AlmacenResponse buscarPorId(Long id) {
        return almacenMapper.toResponse(getAlmacenEntity(id));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "almacenes", allEntries = true)
    public AlmacenResponse crear(AlmacenRequest request) {
        validarNombreUnico(request.nombre(), null);
        return almacenMapper.toResponse(almacenRepository.save(almacenMapper.toEntity(request)));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "almacenes", allEntries = true)
    public AlmacenResponse actualizar(Long id, AlmacenRequest request) {
        Almacen almacen = getAlmacenEntity(id);
        validarNombreUnico(request.nombre(), id);
        almacenMapper.updateEntity(almacen, request);
        return almacenMapper.toResponse(almacenRepository.save(almacen));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'ALMACENERO')")
    @CacheEvict(value = "almacenes", allEntries = true)
    public void eliminar(Long id) {
        Almacen almacen = getAlmacenEntity(id);
        eventPublisher.publishEvent(new AlmacenEliminacionSolicitadaEvent(almacen.getId(), almacen.getNombre()));
        almacenRepository.delete(almacen);
    }

    private void validarNombreUnico(String nombre, Long idActual) {
        boolean existe = (idActual == null)
                ? almacenRepository.existsByNombre(nombre)
                : almacenRepository.existsByNombreAndIdNot(nombre, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe un almacén con el nombre: " + nombre);
        }
    }

    private Almacen getAlmacenEntity(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + id));
    }
}
