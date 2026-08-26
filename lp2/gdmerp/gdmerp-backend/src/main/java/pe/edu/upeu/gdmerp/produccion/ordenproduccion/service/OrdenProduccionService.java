package pe.edu.upeu.gdmerp.produccion.ordenproduccion.service;

import pe.edu.upeu.gdmerp.produccion.ordenproduccion.dto.OrdenProduccionResponse;
import java.util.List;

public interface OrdenProduccionService {
    List<OrdenProduccionResponse> listar();
}