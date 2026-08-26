package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.entity.OrdenProduccion;
import pe.edu.upeu.gdmerp.produccion.ordenproduccion.repository.OrdenProduccionRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrdenProduccionServiceImpl implements OrdenProduccionService {
    private final OrdenProduccionRepository ordenProduccionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrdenProduccionResponse> listar() {
        return ordenProduccionRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OrdenProduccionResponse toResponse(OrdenProduccion orden) {
        return new OrdenProduccionResponse(orden.getId(), orden.getProducto(), orden.getCantidadPlanificada(), orden.getEstado());
    }
}