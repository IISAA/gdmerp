package pe.edu.upeu.gdmerp.inventario.almacen.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;

public interface AlmacenRepository extends JpaRepository<Almacen, Long> {
}