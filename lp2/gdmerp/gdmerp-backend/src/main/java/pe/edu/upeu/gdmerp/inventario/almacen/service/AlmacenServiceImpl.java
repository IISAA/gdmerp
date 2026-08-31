package pe.edu.upeu.gdmerp.inventario.almacen.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.exception.ResourceNotFoundException;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.mapper.AlmacenMapper;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlmacenServiceImpl implements AlmacenService {
    private final AlmacenRepository almacenRepository;
    private final AlmacenMapper almacenMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AlmacenResponse> listar() {
        return almacenRepository.findAll().stream().map(almacenMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AlmacenResponse buscarPorId(Long id) {
        return almacenMapper.toResponse(getAlmacenEntity(id));
    }

    @Override
    @Transactional
    public AlmacenResponse crear(AlmacenRequest request) {
        Almacen almacen = almacenMapper.toEntity(request);
        return almacenMapper.toResponse(almacenRepository.save(almacen));
    }

    @Override
    @Transactional
    public AlmacenResponse actualizar(Long id, AlmacenRequest request) {
        Almacen almacen = getAlmacenEntity(id);
        almacenMapper.updateEntity(almacen, request);
        return almacenMapper.toResponse(almacenRepository.save(almacen));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        almacenRepository.delete(getAlmacenEntity(id));
    }

    private Almacen getAlmacenEntity(Long id) {
        return almacenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado con id " + id));
    }
}