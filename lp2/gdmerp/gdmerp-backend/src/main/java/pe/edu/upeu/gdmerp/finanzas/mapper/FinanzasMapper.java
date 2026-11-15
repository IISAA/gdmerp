package pe.edu.upeu.gdmerp.finanzas.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResumen;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FinanzasMapper {

    CategoriaFinancieraResponse toCategoriaResponse(CategoriaFinanciera entity);

    List<CategoriaFinancieraResponse> toCategoriaResponseList(List<CategoriaFinanciera> entities);

    CategoriaFinancieraResumen toCategoriaResumen(CategoriaFinanciera entity);

    TransaccionFinancieraResponse toTransaccionResponse(TransaccionFinanciera entity);

    List<TransaccionFinancieraResponse> toTransaccionResponseList(List<TransaccionFinanciera> entities);
}
