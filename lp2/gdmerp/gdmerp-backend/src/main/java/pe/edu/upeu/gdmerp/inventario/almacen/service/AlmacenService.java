package pe.edu.upeu.gdmerp.inventario.almacen.service;

import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import java.util.List;

public interface AlmacenService {
    List<AlmacenResponse> listar();
    AlmacenResponse buscarPorId(Long id);
    AlmacenResponse crear(AlmacenRequest request);
    AlmacenResponse actualizar(Long id, AlmacenRequest request);
    void eliminar(Long id);
}