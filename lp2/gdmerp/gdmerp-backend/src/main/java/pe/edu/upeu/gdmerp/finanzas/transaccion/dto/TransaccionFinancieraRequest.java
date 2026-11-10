package pe.edu.upeu.gdmerp.finanzas.transaccion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TransaccionFinancieraRequest {
    @NotBlank(message = "El código de transacción es obligatorio")
    private String codigo;

    @NotBlank(message = "El concepto de la transacción es obligatorio")
    private String concepto;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    private LocalDate fechaTransaccion;

    @NotNull(message = "El tipo financiero (INGRESO/EGRESO) es obligatorio")
    private TipoFinanciero tipo;

    private EstadoFinanciero estado;

    private String tipoComprobante;

    private String numeroComprobante;

    @NotNull(message = "El ID de la categoría financiera es obligatorio")
    private Long categoriaFinancieraId;

    public TransaccionFinancieraRequest() {
    }

    public TransaccionFinancieraRequest(String codigo, String concepto, BigDecimal monto, LocalDate fechaTransaccion, TipoFinanciero tipo, EstadoFinanciero estado, String tipoComprobante, String numeroComprobante, Long categoriaFinancieraId) {
        this.codigo = codigo;
        this.concepto = concepto;
        this.monto = monto;
        this.fechaTransaccion = fechaTransaccion;
        this.tipo = tipo;
        this.estado = estado;
        this.tipoComprobante = tipoComprobante;
        this.numeroComprobante = numeroComprobante;
        this.categoriaFinancieraId = categoriaFinancieraId;
    }
}
