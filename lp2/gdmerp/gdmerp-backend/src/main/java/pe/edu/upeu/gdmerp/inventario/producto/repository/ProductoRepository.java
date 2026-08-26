package pe.edu.upeu.gdmerp.inventario.producto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.gdmerp.inventario.producto.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}