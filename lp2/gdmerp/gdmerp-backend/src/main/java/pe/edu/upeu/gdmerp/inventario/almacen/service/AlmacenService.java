package pe.edu.upeu.gdmerp.inventario.almacen.service;

import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import java.util.List;

public interface AlmacenService {
    List<AlmacenResponse> listar();
}