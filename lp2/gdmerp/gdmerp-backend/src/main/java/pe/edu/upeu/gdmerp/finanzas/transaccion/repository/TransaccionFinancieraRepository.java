package pe.edu.upeu.gdmerp.finanzas.transaccion.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionFinancieraRepository extends JpaRepository<TransaccionFinanciera, Long> {

    Optional<TransaccionFinanciera> findByCodigo(String codigo);

    Optional<TransaccionFinanciera> findFirstByNumeroComprobante(String numeroComprobante);

    Optional<TransaccionFinanciera> findFirstByVentaId(Long ventaId);

    Optional<TransaccionFinanciera> findFirstByCompraId(Long compraId);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    @Override
    @EntityGraph(attributePaths = "categoriaFinanciera")
    List<TransaccionFinanciera> findAll();

    @EntityGraph(attributePaths = "categoriaFinanciera")
    List<TransaccionFinanciera> findByTipo(TipoFinanciero tipo);

    @EntityGraph(attributePaths = "categoriaFinanciera")
    List<TransaccionFinanciera> findByEstado(EstadoFinanciero estado);

    @EntityGraph(attributePaths = "categoriaFinanciera")
    List<TransaccionFinanciera> findByCategoriaFinancieraId(Long categoriaFinancieraId);

    @EntityGraph(attributePaths = "categoriaFinanciera")
    List<TransaccionFinanciera> findByTipoAndEstado(TipoFinanciero tipo, EstadoFinanciero estado);
}
