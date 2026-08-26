package pe.edu.upeu.gdmerp.inventario.almacen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;
import pe.edu.upeu.gdmerp.inventario.almacen.repository.AlmacenRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlmacenServiceImpl implements AlmacenService {
    private final AlmacenRepository almacenRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AlmacenResponse> listar() {
        return almacenRepository.findAll().stream().map(this::toResponse).toList();
    }

    private AlmacenResponse toResponse(Almacen almacen) {
        return new AlmacenResponse(almacen.getId(), almacen.getNombre(), almacen.getUbicacion());
    }
}