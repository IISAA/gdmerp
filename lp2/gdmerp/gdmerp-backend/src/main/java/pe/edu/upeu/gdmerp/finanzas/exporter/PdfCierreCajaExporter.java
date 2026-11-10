package pe.edu.upeu.gdmerp.finanzas.exporter;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.CierreCajaReporteDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MetodoPagoResumenDto;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.MovimientoCajaResponse;
import pe.edu.upeu.gdmerp.finanzas.sesioncaja.dto.SesionCajaResponse;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Component
public class PdfCierreCajaExporter implements CierreCajaExporter {

    private static final Color PRIMARY = new Color(34, 76, 56);      // Verde Agro
    private static final Color SECONDARY = new Color(240, 244, 241); // Fondo suave
    private static final Color TEXT_DARK = new Color(30, 41, 59);

    @Override
    public boolean supports(FormatoReporte formato) {
        return formato == FormatoReporte.PDF;
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    @Override
    public byte[] exportarCierreCaja(CierreCajaReporteDto datos) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PRIMARY);
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font fontSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_DARK);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT_DARK);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            // Encabezado Corporativo
            Paragraph title = new Paragraph("AGRO PERUVIAN FOODS S.A.C.", fontTitulo);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph subtitle = new Paragraph("ACTA OFICIAL DE CORTE DE CAJA Y ARQUEO DIARIO\nRUC: " + datos.getRuc() + " | " + datos.getDireccion(), fontSub);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15);
            doc.add(subtitle);

            // Datos de la Sesión
            SesionCajaResponse sesion = datos.getSesion();
            PdfPTable infoTable = new PdfPTable(4);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15);

            addCellInfo(infoTable, "Código Sesión:", sesion.getCodigoSesion(), fontBold, fontBody);
            addCellInfo(infoTable, "Cajero Responsable:", sesion.getCajero(), fontBold, fontBody);
            addCellInfo(infoTable, "Apertura:", sesion.getFechaApertura() != null ? sesion.getFechaApertura().format(fmt) : "-", fontBold, fontBody);
            addCellInfo(infoTable, "Cierre:", sesion.getFechaCierre() != null ? sesion.getFechaCierre().format(fmt) : "EN CURSO", fontBold, fontBody);
            addCellInfo(infoTable, "Estado de Caja:", sesion.getEstado() != null ? sesion.getEstado().name() : "-", fontBold, fontBody);
            addCellInfo(infoTable, "Observaciones:", sesion.getObservaciones() != null ? sesion.getObservaciones() : "Sin observaciones", fontBold, fontBody);
            doc.add(infoTable);

            // Resumen de Arqueo de Efectivo
            doc.add(new Paragraph("CONCILIACIÓN Y ARQUEO EN EFECTIVO (GAVETA)", fontSeccion));
            PdfPTable arqueoTable = new PdfPTable(6);
            arqueoTable.setWidthPercentage(100);
            arqueoTable.setSpacingBefore(5);
            arqueoTable.setSpacingAfter(15);

            addHeaderCell(arqueoTable, "Saldo Inicial", fontHeader);
            addHeaderCell(arqueoTable, "(+) Ingresos", fontHeader);
            addHeaderCell(arqueoTable, "(-) Egresos", fontHeader);
            addHeaderCell(arqueoTable, "Saldo Esperado", fontHeader);
            addHeaderCell(arqueoTable, "Saldo Real (Físico)", fontHeader);
            addHeaderCell(arqueoTable, "Diferencia", fontHeader);

            addAmountCell(arqueoTable, sesion.getSaldoInicial(), fontBody);
            addAmountCell(arqueoTable, sesion.getTotalIngresosEfectivo(), fontBody);
            addAmountCell(arqueoTable, sesion.getTotalEgresosEfectivo(), fontBody);
            addAmountCell(arqueoTable, sesion.getSaldoEsperadoEfectivo(), fontBold);
            addAmountCell(arqueoTable, sesion.getSaldoRealEfectivo(), fontBold);
            addAmountCell(arqueoTable, sesion.getDiferencia(), fontBold);
            doc.add(arqueoTable);

            // Resumen por Método de Pago
            if (datos.getResumenPorMetodo() != null && !datos.getResumenPorMetodo().isEmpty()) {
                doc.add(new Paragraph("DISTRIBUCIÓN POR MÉTODO DE PAGO", fontSeccion));
                PdfPTable metodoTable = new PdfPTable(4);
                metodoTable.setWidthPercentage(100);
                metodoTable.setSpacingBefore(5);
                metodoTable.setSpacingAfter(15);

                addHeaderCell(metodoTable, "Método de Pago", fontHeader);
                addHeaderCell(metodoTable, "Total Ingresos", fontHeader);
                addHeaderCell(metodoTable, "Total Egresos", fontHeader);
                addHeaderCell(metodoTable, "Balance Neto", fontHeader);

                for (MetodoPagoResumenDto mp : datos.getResumenPorMetodo()) {
                    metodoTable.addCell(new Phrase(mp.getMetodoPago().name(), fontBody));
                    addAmountCell(metodoTable, mp.getTotalIngresos(), fontBody);
                    addAmountCell(metodoTable, mp.getTotalEgresos(), fontBody);
                    addAmountCell(metodoTable, mp.getNeto(), fontBold);
                }
                doc.add(metodoTable);
            }

            // Detalle de Movimientos
            if (datos.getMovimientos() != null && !datos.getMovimientos().isEmpty()) {
                doc.add(new Paragraph("DETALLE DE MOVIMIENTOS REGISTRADOS", fontSeccion));
                PdfPTable movTable = new PdfPTable(6);
                movTable.setWidthPercentage(100);
                movTable.setWidths(new float[]{15, 12, 14, 25, 20, 14});
                movTable.setSpacingBefore(5);
                movTable.setSpacingAfter(20);

                addHeaderCell(movTable, "Fecha/Hora", fontHeader);
                addHeaderCell(movTable, "Tipo", fontHeader);
                addHeaderCell(movTable, "Método", fontHeader);
                addHeaderCell(movTable, "Categoría", fontHeader);
                addHeaderCell(movTable, "Descripción", fontHeader);
                addHeaderCell(movTable, "Monto", fontHeader);

                for (MovimientoCajaResponse mov : datos.getMovimientos()) {
                    movTable.addCell(new Phrase(mov.getFechaHora() != null ? mov.getFechaHora().format(fmt) : "-", fontBody));
                    movTable.addCell(new Phrase(mov.getTipo() != null ? mov.getTipo().name() : "-", fontBody));
                    movTable.addCell(new Phrase(mov.getMetodoPago() != null ? mov.getMetodoPago().name() : "-", fontBody));
                    movTable.addCell(new Phrase(mov.getCategoriaFinancieraNombre() != null ? mov.getCategoriaFinancieraNombre() : "-", fontBody));
                    movTable.addCell(new Phrase(mov.getDescripcion() != null ? mov.getDescripcion() : "-", fontBody));
                    addAmountCell(movTable, mov.getMonto(), fontBody);
                }
                doc.add(movTable);
            }

            // Firmas de Conformidad
            Paragraph firmas = new Paragraph("\n\n____________________________              ____________________________\n       Firma del Cajero                          Firma del Supervisor\n", fontSub);
            firmas.setAlignment(Element.ALIGN_CENTER);
            doc.add(firmas);

            doc.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Error al generar PDF de cierre de caja: " + ex.getMessage(), ex);
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY);
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addCellInfo(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBackgroundColor(SECONDARY);
        c1.setBorder(0);
        c1.setPadding(4);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(0);
        c2.setPadding(4);
        table.addCell(c2);
    }

    private void addAmountCell(PdfPTable table, BigDecimal amount, Font font) {
        String txt = amount != null ? String.format("S/ %.2f", amount) : "S/ 0.00";
        PdfPCell cell = new PdfPCell(new Phrase(txt, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
