package pe.edu.upeu.gdmerp.catalogo.producto.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;

import java.util.List;
import java.util.Optional;

@Repository("catalogoProductoRepository")
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Override
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findAll();

    @Override
    @EntityGraph(attributePaths = "categoria")
    Optional<Producto> findById(Long id);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByCategoria_Id(Long categoriaId);
}
