package pe.edu.upeu.gdmerp.inventario.almacen.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenRequest;
import pe.edu.upeu.gdmerp.inventario.almacen.dto.AlmacenResponse;
import pe.edu.upeu.gdmerp.inventario.almacen.entity.Almacen;

@Mapper(componentModel = "spring")
public interface AlmacenMapper {
    AlmacenResponse toResponse(Almacen almacen);
    Almacen toEntity(AlmacenRequest request);
    void updateEntity(@MappingTarget Almacen almacen, AlmacenRequest request);
}