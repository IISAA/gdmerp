package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.producto.dto.CostoProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoQueryServiceImpl implements ProductoQueryService {

    private final ProductoRepository productoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CostoProductoDTO> obtenerCostoPromedioPorIds(List<Long> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return List.of();
        }
        return productoRepository.findAllById(productoIds)
                .stream()
                .map(p -> new CostoProductoDTO(p.getId(), p.getCostoPromedio()))
                .toList();
    }
}