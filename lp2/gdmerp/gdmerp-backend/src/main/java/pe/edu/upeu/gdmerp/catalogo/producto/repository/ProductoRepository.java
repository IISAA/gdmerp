package pe.edu.upeu.gdmerp.catalogo.producto.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.catalogo.producto.dto.TipoProducto;
import pe.edu.upeu.gdmerp.catalogo.producto.entity.Producto;

import java.util.Collection;
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

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByTipoProducto(TipoProducto tipoProducto);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByIdIn(Collection<Long> ids);

    Optional<Producto> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    long countBySkuStartingWith(String prefijo);
}
