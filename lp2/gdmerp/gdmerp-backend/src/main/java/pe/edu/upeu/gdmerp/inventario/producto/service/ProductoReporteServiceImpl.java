package pe.edu.upeu.gdmerp.inventario.producto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockCriticoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.dto.ReporteStockProductoDTO;
import pe.edu.upeu.gdmerp.inventario.producto.repository.ProductoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoReporteServiceImpl implements ProductoReporteService {

    private final ProductoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ReporteStockProductoDTO> obtenerReporteStock() {
        return repository.reporteStockConsolidado();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteStockCriticoDTO> obtenerReporteStockCritico() {
        return repository.reporteStockCritico();
    }
}