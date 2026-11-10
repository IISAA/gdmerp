package pe.edu.upeu.gdmerp.finanzas.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.CategoriaFinancieraResumen;
import pe.edu.upeu.gdmerp.finanzas.transaccion.dto.TransaccionFinancieraResponse;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.CategoriaFinanciera;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TransaccionFinanciera;

@Component
public class FinanzasMapperImpl implements FinanzasMapper {

    @Override
    public CategoriaFinancieraResponse toCategoriaResponse(CategoriaFinanciera entity) {
        if (entity == null) {
            return null;
        }
        return CategoriaFinancieraResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .tipoDefault(entity.getTipoDefault())
                .build();
    }

    @Override
    public CategoriaFinancieraResumen toCategoriaResumen(CategoriaFinanciera entity) {
        if (entity == null) {
            return null;
        }
        return CategoriaFinancieraResumen.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .tipoDefault(entity.getTipoDefault())
                .build();
    }

    @Override
    public TransaccionFinancieraResponse toTransaccionResponse(TransaccionFinanciera entity) {
        if (entity == null) {
            return null;
        }
        return TransaccionFinancieraResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .concepto(entity.getConcepto())
                .monto(entity.getMonto())
                .fechaTransaccion(entity.getFechaTransaccion())
                .tipo(entity.getTipo())
                .estado(entity.getEstado())
                .tipoComprobante(entity.getTipoComprobante())
                .numeroComprobante(entity.getNumeroComprobante())
                .categoriaFinanciera(toCategoriaResumen(entity.getCategoriaFinanciera()))
                .build();
    }
}
