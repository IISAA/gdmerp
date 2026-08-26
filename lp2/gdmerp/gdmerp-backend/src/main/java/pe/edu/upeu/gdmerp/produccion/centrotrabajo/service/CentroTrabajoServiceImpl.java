package pe.edu.upeu.gdmerp.produccion.centrotrabajo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.entity.CentroTrabajo;
import pe.edu.upeu.gdmerp.produccion.centrotrabajo.repository.CentroTrabajoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroTrabajoServiceImpl implements CentroTrabajoService {
    private final CentroTrabajoRepository centroTrabajoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CentroTrabajoResponse> listar() {
        return centroTrabajoRepository.findAll().stream().map(this::toResponse).toList();
    }

    private CentroTrabajoResponse toResponse(CentroTrabajo centroTrabajo) {
        return new CentroTrabajoResponse(centroTrabajo.getId(), centroTrabajo.getNombre(), centroTrabajo.getCapacidad());
    }
}