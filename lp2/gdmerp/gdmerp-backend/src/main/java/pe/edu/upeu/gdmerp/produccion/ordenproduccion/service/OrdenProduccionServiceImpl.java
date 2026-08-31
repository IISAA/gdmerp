package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionRequest;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.mapper.OrdenProduccionMapper;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {
    private final OrdenProduccionRepository repository;
    private final OrdenProduccionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listar() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenProduccionResponse buscarPorId(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse crear(OrdenProduccionRequest request) {
        OrdenProduccion op = mapper.toEntity(request);
        return mapper.toResponse(repository.save(op));
    }

    @Override
    @Transactional
    public OrdenProduccionResponse actualizar(Long id, OrdenProduccionRequest request) {
        OrdenProduccion op = getEntity(id);
        mapper.updateEntity(op, request);
        return mapper.toResponse(repository.save(op));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        repository.delete(getEntity(id));
    }

    private OrdenProduccion getEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden de Producción no encontrada: " + id));
    }
}