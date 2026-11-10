package pe.edu.upeu.gdmerp.finanzas.mapper;

import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResumen;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;

public interface FinanzasMapper {

    CategoriaFinancieraResponse toCategoriaResponse(CategoriaFinanciera entity);

    CategoriaFinancieraResumen toCategoriaResumen(CategoriaFinanciera entity);

    TransaccionFinancieraResponse toTransaccionResponse(TransaccionFinanciera entity);
}
