package pe.edu.upeu.gdmerp.finanzas.exporter;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class ExcelCierreCajaExporter implements CierreCajaExporter {

    @Override
    public boolean supports(FormatoReporte formato) {
        return formato == FormatoReporte.EXCEL;
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    @Override
    public byte[] exportarCierreCaja(CierreCajaReporteDto datos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // Estilos
            Font fontHeader = workbook.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());

            CellStyle styleHeader = workbook.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.CENTER);
            styleHeader.setBorderBottom(BorderStyle.THIN);

            CellStyle styleCurrency = workbook.createCellStyle();
            styleCurrency.setDataFormat(workbook.createDataFormat().getFormat("S/ #,##0.00"));

            Font boldF = workbook.createFont();
            boldF.setBold(true);
            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldF);

            // ==========================================
            // HOJA 1: RESUMEN DE TURNO
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Resumen Turno");
            sheet1.setColumnWidth(0, 7000);
            sheet1.setColumnWidth(1, 8000);
            sheet1.setColumnWidth(2, 6000);
            sheet1.setColumnWidth(3, 6000);

            Row r0 = sheet1.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("AGRO PERUVIAN FOODS S.A.C. - CORTE DE CAJA");
            c0.setCellStyle(boldStyle);

            SesionCajaResponse s = datos.getSesion();
            int rowIdx = 2;
            addRowText(sheet1, rowIdx++, "Código de Sesión:", s.getCodigoSesion());
            addRowText(sheet1, rowIdx++, "Cajero Responsable:", s.getCajero());
            addRowText(sheet1, rowIdx++, "Fecha Apertura:", s.getFechaApertura() != null ? s.getFechaApertura().format(fmt) : "-");
            addRowText(sheet1, rowIdx++, "Fecha Cierre:", s.getFechaCierre() != null ? s.getFechaCierre().format(fmt) : "EN CURSO");
            addRowText(sheet1, rowIdx++, "Estado:", s.getEstado() != null ? s.getEstado().name() : "-");
            addRowText(sheet1, rowIdx++, "Observaciones:", s.getObservaciones() != null ? s.getObservaciones() : "-");

            rowIdx++;
            Row rArqueoTitle = sheet1.createRow(rowIdx++);
            Cell cArqueo = rArqueoTitle.createCell(0);
            cArqueo.setCellValue("ARQUEO DE EFECTIVO (GAVETA)");
            cArqueo.setCellStyle(boldStyle);

            addRowAmount(sheet1, rowIdx++, "Saldo Inicial en Efectivo:", s.getSaldoInicial(), styleCurrency);
            addRowAmount(sheet1, rowIdx++, "(+) Total Ingresos en Efectivo:", s.getTotalIngresosEfectivo(), styleCurrency);
            addRowAmount(sheet1, rowIdx++, "(-) Total Egresos en Efectivo:", s.getTotalEgresosEfectivo(), styleCurrency);
            addRowAmount(sheet1, rowIdx++, "(=) Saldo Esperado del Sistema:", s.getSaldoEsperadoEfectivo(), styleCurrency);
            addRowAmount(sheet1, rowIdx++, "(=) Saldo Real Físico (Arqueado):", s.getSaldoRealEfectivo(), styleCurrency);
            addRowAmount(sheet1, rowIdx++, "Diferencia (Sobrante/Faltante):", s.getDiferencia(), styleCurrency);

            rowIdx += 2;
            Row rMetHeader = sheet1.createRow(rowIdx++);
            String[] metCols = {"Método de Pago", "Ingresos", "Egresos", "Neto"};
            for (int i = 0; i < metCols.length; i++) {
                Cell ch = rMetHeader.createCell(i);
                ch.setCellValue(metCols[i]);
                ch.setCellStyle(styleHeader);
            }

            if (datos.getResumenPorMetodo() != null) {
                for (MetodoPagoResumenDto mp : datos.getResumenPorMetodo()) {
                    Row rm = sheet1.createRow(rowIdx++);
                    rm.createCell(0).setCellValue(mp.getMetodoPago().name());
                    setAmount(rm.createCell(1), mp.getTotalIngresos(), styleCurrency);
                    setAmount(rm.createCell(2), mp.getTotalEgresos(), styleCurrency);
                    setAmount(rm.createCell(3), mp.getNeto(), styleCurrency);
                }
            }

            // ==========================================
            // HOJA 2: DETALLE DE MOVIMIENTOS
            // ==========================================
            Sheet sheet2 = workbook.createSheet("Detalle Movimientos");
            sheet2.setColumnWidth(0, 5000);
            sheet2.setColumnWidth(1, 3500);
            sheet2.setColumnWidth(2, 4500);
            sheet2.setColumnWidth(3, 7000);
            sheet2.setColumnWidth(4, 9000);
            sheet2.setColumnWidth(5, 4500);
            sheet2.setColumnWidth(6, 4500);

            Row rMovH = sheet2.createRow(0);
            String[] movHeaders = {"Fecha y Hora", "Tipo", "Método Pago", "Categoría", "Descripción", "Referencia", "Monto"};
            for (int i = 0; i < movHeaders.length; i++) {
                Cell ch = rMovH.createCell(i);
                ch.setCellValue(movHeaders[i]);
                ch.setCellStyle(styleHeader);
            }

            int mRowIdx = 1;
            if (datos.getMovimientos() != null) {
                for (MovimientoCajaResponse mov : datos.getMovimientos()) {
                    Row mr = sheet2.createRow(mRowIdx++);
                    mr.createCell(0).setCellValue(mov.getFechaHora() != null ? mov.getFechaHora().format(fmt) : "-");
                    mr.createCell(1).setCellValue(mov.getTipo() != null ? mov.getTipo().name() : "-");
                    mr.createCell(2).setCellValue(mov.getMetodoPago() != null ? mov.getMetodoPago().name() : "-");
                    mr.createCell(3).setCellValue(mov.getCategoriaFinancieraNombre() != null ? mov.getCategoriaFinancieraNombre() : "-");
                    mr.createCell(4).setCellValue(mov.getDescripcion() != null ? mov.getDescripcion() : "-");
                    mr.createCell(5).setCellValue(mov.getReferenciaExterna() != null ? mov.getReferenciaExterna() : "-");
                    setAmount(mr.createCell(6), mov.getMonto(), styleCurrency);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error al generar Excel de cierre de caja: " + ex.getMessage(), ex);
        }
    }

    private void addRowText(Sheet sheet, int rowIdx, String label, String val) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(label);
        r.createCell(1).setCellValue(val != null ? val : "");
    }

    private void addRowAmount(Sheet sheet, int rowIdx, String label, BigDecimal amount, CellStyle style) {
        Row r = sheet.createRow(rowIdx);
        r.createCell(0).setCellValue(label);
        Cell c = r.createCell(1);
        setAmount(c, amount, style);
    }

    private void setAmount(Cell cell, BigDecimal amount, CellStyle style) {
        if (amount != null) {
            cell.setCellValue(amount.doubleValue());
        } else {
            cell.setCellValue(0.0);
        }
        cell.setCellStyle(style);
    }
}
