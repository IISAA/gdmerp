package pe.edu.upeu.gdmerp.inventario.categoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.gdmerp.inventario.categoria.entity.Categoria;
import pe.edu.upeu.gdmerp.inventario.categoria.repository.CategoriaRepository;

import java.util.List;
import java.util.Optional;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class CategoriaDataInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // 1. Si existe una categoría llamada 'Producto Terminado', renombrarla a familia comercial para evitar ambigüedad con TipoProducto
            Optional<Categoria> catProductoTerminado = categoriaRepository.findByNombreIgnoreCase("Producto Terminado");
            if (catProductoTerminado.isPresent()) {
                Categoria cat = catProductoTerminado.get();
                cat.setNombre("Tabletas y Chocolates");
                cat.setDescripcion("Chocolates finos de aroma y coberturas");
                categoriaRepository.save(cat);
                log.info("Categoría 'Producto Terminado' renombrada con éxito a 'Tabletas y Chocolates'.");
            }

            // 2. Familias comerciales oficiales de Agro Peruvian Foods
            List<CategoriaSeed> categoriasCatalogo = List.of(
                new CategoriaSeed("Derivados de Cacao", "Nibs, manteca, licor y polvo de cacao"),
                new CategoriaSeed("Aceites y Grasas", "Aceite de coco virgen, sacha inchi y derivados oleosos"),
                new CategoriaSeed("Tabletas y Chocolates", "Chocolates finos de aroma y coberturas"),
                new CategoriaSeed("Envases y Botellas", "Frascos de vidrio, tapas, etiquetas y empaques"),
                new CategoriaSeed("Endulzantes", "Panela granulada, miel de abeja y néctar de agave"),
                new CategoriaSeed("Granos y Cereales", "Quinua blanca, roja, negra, kiwicha y chía")
            );

            for (CategoriaSeed seed : categoriasCatalogo) {
                if (!categoriaRepository.existsByNombre(seed.nombre())) {
                    Categoria nueva = new Categoria();
                    nueva.setNombre(seed.nombre());
                    nueva.setDescripcion(seed.descripcion());
                    categoriaRepository.save(nueva);
                    log.info("Familia comercial creada: {}", seed.nombre());
                }
            }
        } catch (Exception e) {
            log.warn("Inicialización de categorías omitida o ya existente: {}", e.getMessage());
        }
    }

    private record CategoriaSeed(String nombre, String descripcion) {}
}
