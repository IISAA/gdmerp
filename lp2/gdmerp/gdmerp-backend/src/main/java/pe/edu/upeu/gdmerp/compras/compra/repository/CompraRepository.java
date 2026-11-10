package pe.edu.upeu.gdmerp.compras.compra.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.compras.compra.entity.Compra;
import pe.edu.upeu.gdmerp.compras.compra.entity.EstadoCompra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Override
    @EntityGraph(attributePaths = {"proveedor", "detalles"})
    List<Compra> findAll();

    @Override
    @EntityGraph(attributePaths = {"proveedor", "detalles"})
    Optional<Compra> findById(Long id);

    @EntityGraph(attributePaths = {"proveedor", "detalles"})
    @Query("SELECT c FROM Compra c WHERE (:estado IS NULL OR c.estado = :estado) " +
           "AND (CAST(:desde AS date) IS NULL OR c.fechaEmision >= :desde) " +
           "AND (CAST(:hasta AS date) IS NULL OR c.fechaEmision <= :hasta) " +
           "ORDER BY c.fechaEmision DESC, c.id DESC")
    List<Compra> buscar(@Param("estado") EstadoCompra estado,
                        @Param("desde") LocalDate desde,
                        @Param("hasta") LocalDate hasta);
}
