package pe.edu.upeu.gdmerp.finanzas.transaccion.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.EstadoFinanciero;
import pe.edu.upeu.gdmerp.finanzas.transaccion.entity.TipoFinanciero;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class TransaccionFinancieraResponse {
    private Long id;
    private String codigo;
    private String concepto;
    private BigDecimal monto;
    private LocalDate fechaTransaccion;
    private TipoFinanciero tipo;
    private EstadoFinanciero estado;
    private String tipoComprobante;
    private String numeroComprobante;
    private CategoriaFinancieraResumen categoriaFinanciera;

    public TransaccionFinancieraResponse() {
    }

    public TransaccionFinancieraResponse(Long id, String codigo, String concepto, BigDecimal monto, LocalDate fechaTransaccion, TipoFinanciero tipo, EstadoFinanciero estado, String tipoComprobante, String numeroComprobante, CategoriaFinancieraResumen categoriaFinanciera) {
        this.id = id;
        this.codigo = codigo;
        this.concepto = concepto;
        this.monto = monto;
        this.fechaTransaccion = fechaTransaccion;
        this.tipo = tipo;
        this.estado = estado;
        this.tipoComprobante = tipoComprobante;
        this.numeroComprobante = numeroComprobante;
        this.categoriaFinanciera = categoriaFinanciera;
    }
}
