package pe.edu.upeu.gdmerp.produccion.centrotrabajo.service;

import pe.edu.upeu.gdmerp.produccion.centrotrabajo.dto.CentroTrabajoResponse;
import java.util.List;

public interface CentroTrabajoService {
    List<CentroTrabajoResponse> listar();
}