package pe.edu.upeu.gdmerp.inventario.lote.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteRequest;
import pe.edu.upeu.gdmerp.inventario.lote.dto.LoteResponse;
import pe.edu.upeu.gdmerp.inventario.lote.entity.Lote;

@Mapper(componentModel = "spring")
public interface LoteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "producto", ignore = true)
    @Mapping(target = "version", ignore = true)
    Lote toEntity(LoteRequest request);

    @Mapping(source = "producto.id", target = "productoId")
    @Mapping(source = "producto.nombre", target = "productoNombre")
    LoteResponse toResponse(Lote lote);
}