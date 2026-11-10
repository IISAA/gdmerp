package pe.edu.upeu.gdmerp.catalogo.categoria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upeu.gdmerp.catalogo.categoria.entity.Categoria;

@Repository("catalogoCategoriaRepository")
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
