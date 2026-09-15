package pe.edu.upeu.gdmerp.produccion.centrotrabajo.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoRequest;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.mapper.CentroTrabajoMapper;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroTrabajoServiceImpl implements CentroTrabajoService {
    private final CentroTrabajoRepository repository;
    private final OrdenProduccionRepository ordenProduccionRepository;
    private final CentroTrabajoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CentroTrabajoResponse> listar() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CentroTrabajoResponse buscarPorId(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public CentroTrabajoResponse crear(CentroTrabajoRequest request) {
        validarNombreUnico(request.nombre(), null);
        CentroTrabajo ct = mapper.toEntity(request);
        return mapper.toResponse(repository.save(ct));
    }

    @Override
    @Transactional
    public CentroTrabajoResponse actualizar(Long id, CentroTrabajoRequest request) {
        CentroTrabajo ct = getEntity(id);
        validarNombreUnico(request.nombre(), id);
        mapper.updateEntity(ct, request);
        return mapper.toResponse(repository.save(ct));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        CentroTrabajo ct = getEntity(id);
        if (ordenProduccionRepository.existsByCentroTrabajoId(id)) {
            throw new IllegalStateException("No se puede eliminar el centro de trabajo con id " + id + " porque tiene órdenes de producción asociadas");
        }
        repository.delete(ct);
    }

    private void validarNombreUnico(String nombre, Long idActual) {
        boolean existe = (idActual == null)
                ? repository.existsByNombre(nombre)
                : repository.existsByNombreAndIdNot(nombre, idActual);
        if (existe) {
            throw new IllegalStateException("Ya existe un centro de trabajo con el nombre: " + nombre);
        }
    }

    private CentroTrabajo getEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Centro de Trabajo no encontrado: " + id));
    }
}