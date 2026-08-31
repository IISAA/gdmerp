package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {
    private final OrdenProduccionRepository ordenRepository;
    private final CentroTrabajoRepository centroTrabajoRepository;
    private final OrdenProduccionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listar() {
        return ordenRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponse buscarPorId(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listarPorCentroTrabajo(Long centroTrabajoId) {
        getCentroTrabajoEntity(centroTrabajoId); // Valida que el centro exista (404 si no)
        return ordenRepository.findByCentroTrabajoId(centroTrabajoId).stream()
            .map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {
        OrdenProduccion op = mapper.toEntity(request);
        op.setCentroTrabajo(getCentroTrabajoEntity(request.centroTrabajoId()));
        return mapper.toResponse(ordenRepository.save(op));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse actualizar(Long id, OrdenProduccionRequest request) {
        OrdenProduccion op = getEntity(id);
        mapper.updateEntity(op, request);
        op.setCentroTrabajo(getCentroTrabajoEntity(request.centroTrabajoId()));
        return mapper.toResponse(ordenRepository.save(op));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        ordenRepository.delete(getEntity(id));
    }

    private OrdenProduccion getEntity(Long id) {
        return ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + id));
    }

    private CentroTrabajo getCentroTrabajoEntity(Long id) {
        return centroTrabajoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Centro de Trabajo no encontrado: " + id));
    }
}
