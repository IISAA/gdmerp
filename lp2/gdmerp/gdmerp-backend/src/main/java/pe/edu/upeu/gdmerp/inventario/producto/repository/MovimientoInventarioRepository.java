package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import pe.edu.upeu.gdmerp.inventario.producto.entity.MovimientoInventario;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {
}